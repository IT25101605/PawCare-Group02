package com.pawcare.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps grooming bookings compatible with databases created by older PawCare ZIPs.
 *
 * New versions keep the complete service selection directly on grooming_appointments
 * (selected_service_ids / selected_service_names).  This means a customer booking no
 * longer depends on a many-to-many join table being present or writable.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GroomingSchemaCompatibilityInitializer implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(GroomingSchemaCompatibilityInitializer.class);
    private final JdbcTemplate jdbc;

    public GroomingSchemaCompatibilityInitializer(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Override
    public void run(ApplicationArguments args){
        try{
            ensureColumn("total_duration_minutes","INT NULL");
            ensureColumn("selected_service_ids","VARCHAR(1000) NULL");
            ensureColumn("selected_service_names","VARCHAR(2000) NULL");
            ensureColumn("comfort_profile","VARCHAR(20) NULL");
            ensureColumn("preferred_finish","VARCHAR(30) NULL");
            ensureColumn("completed_tasks","VARCHAR(250) NULL");
            ensureColumn("products_used","VARCHAR(500) NULL");
            ensureColumn("next_visit_date","DATE NULL");
            backfillLegacyRows();
        }catch(Exception ex){
            // Hibernate ddl-auto=update normally creates these columns already.  Log a precise
            // diagnostic without preventing the rest of PawCare from starting.
            log.warn("Grooming compatibility check could not complete: {}",ex.getMessage());
        }
    }

    private void ensureColumn(String column,String definition){
        // Read the table's actual columns rather than DATABASE()/information_schema:
        // database catalog and schema names differ between MySQL and the test database.
        Boolean exists=jdbc.query("SELECT * FROM grooming_appointments WHERE 1=0",rs->{
            var metadata=rs.getMetaData();
            for(int i=1;i<=metadata.getColumnCount();i++){
                if(column.equalsIgnoreCase(metadata.getColumnName(i)))return true;
            }
            return false;
        });
        if(!Boolean.TRUE.equals(exists))
            jdbc.execute("ALTER TABLE grooming_appointments ADD COLUMN "+column+" "+definition);
    }

    private boolean tableExists(String table){
        return Boolean.TRUE.equals(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)connection->{
            try(var tables=connection.getMetaData().getTables(connection.getCatalog(),null,"%",new String[]{"TABLE"})){
                while(tables.next())if(table.equalsIgnoreCase(tables.getString("TABLE_NAME")))return true;
            }
            return false;
        }));
    }

    private void backfillLegacyRows(){
        // Migrate the join selection before the primary-service fallback. Only rows with
        // no snapshot may be migrated: old join rows must never undo later customer edits.
        if(tableExists("grooming_appointment_services")){
            var legacy=jdbc.queryForList("""
                SELECT gas.grooming_appointment_id AS appointment_id,
                       gas.grooming_service_id AS service_id, gs.name, gs.duration_minutes
                FROM grooming_appointment_services gas
                JOIN grooming_services gs ON gs.id=gas.grooming_service_id
                JOIN grooming_appointments ga ON ga.id=gas.grooming_appointment_id
                WHERE ga.selected_service_ids IS NULL OR TRIM(ga.selected_service_ids)=''
                ORDER BY gas.grooming_appointment_id,gas.grooming_service_id
                """);
            var grouped=new java.util.LinkedHashMap<Long,java.util.List<java.util.Map<String,Object>>>();
            for(var row:legacy){
                long id=((Number)row.get("appointment_id")).longValue();
                grouped.computeIfAbsent(id,ignored->new java.util.ArrayList<>()).add(row);
            }
            grouped.forEach((id,rows)->{
                String ids=rows.stream().map(row->row.get("service_id").toString()).collect(java.util.stream.Collectors.joining(","));
                String names=rows.stream().map(row->java.util.Objects.toString(row.get("name"),"")).collect(java.util.stream.Collectors.joining(" + "));
                int duration=rows.stream().mapToInt(row->row.get("duration_minutes") instanceof Number n?n.intValue():0).sum();
                jdbc.update("""
                    UPDATE grooming_appointments
                    SET selected_service_ids=?,selected_service_names=?,total_duration_minutes=?
                    WHERE id=? AND (selected_service_ids IS NULL OR TRIM(selected_service_ids)='')
                    """,ids,names,Math.max(15,duration),id);
            });
        }
        var primary=jdbc.queryForList("""
            SELECT ga.id,ga.service_id,gs.name,gs.duration_minutes
            FROM grooming_appointments ga LEFT JOIN grooming_services gs ON gs.id=ga.service_id
            WHERE ga.service_id IS NOT NULL
            """);
        for(var row:primary){
            int duration=row.get("duration_minutes") instanceof Number n?n.intValue():30;
            jdbc.update("""
                UPDATE grooming_appointments
                SET selected_service_ids=COALESCE(NULLIF(TRIM(selected_service_ids),''),?),
                    selected_service_names=COALESCE(NULLIF(TRIM(selected_service_names),''),?),
                    total_duration_minutes=CASE WHEN total_duration_minutes IS NULL OR total_duration_minutes<=0 THEN ? ELSE total_duration_minutes END
                WHERE id=?
                """,row.get("service_id").toString(),row.get("name"),Math.max(15,duration),row.get("id"));
        }
    }
}
