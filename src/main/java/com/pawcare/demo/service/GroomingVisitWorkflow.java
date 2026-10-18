package com.pawcare.demo.service;

import com.pawcare.demo.entity.GroomingAppointment;
import com.pawcare.demo.exception.BadRequestException;
import java.util.*;

/** Builds a short, ordered checklist from the booked service-name snapshot. */
public final class GroomingVisitWorkflow {
    private GroomingVisitWorkflow() {}
    private static final Map<String,String> LABELS = Map.ofEntries(
            Map.entry("CHECK_IN","Checked in"), Map.entry("BATH","Bathing"),
            Map.entry("DRY","Drying"), Map.entry("BRUSH","Brushing"),
            Map.entry("STYLE","Trimming & styling"), Map.entry("NAILS","Nail care"),
            Map.entry("EARS","Ear care"), Map.entry("TEETH","Teeth cleaning"),
            Map.entry("SERVICE","Selected service"), Map.entry("FINAL_CHECK","Final inspection"));
    private static final List<String> ORDER = List.of("CHECK_IN","BATH","DRY","BRUSH","STYLE","NAILS","EARS","TEETH","SERVICE","FINAL_CHECK");
    public record Task(String code, String label, boolean completed) {}

    public static List<String> required(GroomingAppointment visit) {
        String name = Optional.ofNullable(visit.getSelectedServiceNames()).orElseGet(
                () -> visit.getService() == null ? "" : visit.getService().getName()).toLowerCase(Locale.ROOT);
        Set<String> keys = new HashSet<>(List.of("CHECK_IN","FINAL_CHECK"));
        if (name.contains("bath") || name.contains("full grooming") || name.contains("shedding") || name.contains("flea")) keys.addAll(List.of("BATH","DRY"));
        if (name.contains("bath") || name.contains("full grooming") || name.contains("shedding")) keys.add("BRUSH");
        if (supportsStyle(name)) keys.add("STYLE");
        if (name.contains("nail")) keys.add("NAILS");
        if (name.contains("ear")) keys.add("EARS");
        if (name.contains("teeth")) keys.add("TEETH");
        if (keys.size() == 2) keys.add("SERVICE");
        return ORDER.stream().filter(keys::contains).toList();
    }
    public static List<String> completed(GroomingAppointment visit) {
        if (visit.getCompletedTasks() == null || visit.getCompletedTasks().isBlank()) return List.of();
        return Arrays.asList(visit.getCompletedTasks().split(","));
    }
    public static boolean supportsStyle(String names) {
        return Arrays.stream(names.toLowerCase(Locale.ROOT).split("\\s*\\+\\s*")).anyMatch(
                name -> name.contains("full grooming") || name.contains("haircut") || (name.contains("trim") && !name.contains("nail")));
    }
    public static List<Task> tasks(GroomingAppointment visit) {
        var done = completed(visit);
        return required(visit).stream().map(key -> new Task(key,LABELS.get(key),done.contains(key))).toList();
    }
    public static void saveTasks(GroomingAppointment visit, List<String> requested) {
        if (visit.getStatus() != GroomingAppointment.GroomingStatus.IN_PROGRESS)
            throw new BadRequestException("Start the visit before updating its checklist");
        var required = required(visit);
        if (requested.size() > required.size() || !requested.equals(required.subList(0,requested.size())))
            throw new BadRequestException("Complete the visit tasks in order");
        visit.setCompletedTasks(String.join(",",requested));
    }
    public static String progress(GroomingAppointment visit) {
        return switch (visit.getStatus()) {
            case SCHEDULED -> "Booked";
            case CONFIRMED -> "Confirmed";
            case CANCELLED -> "Cancelled";
            case COMPLETED -> "Ready for pickup";
            case IN_PROGRESS -> {
                if (visit.getCompletedTasks() == null) yield "In progress";
                var done = completed(visit);
                yield required(visit).stream().filter(key -> !done.contains(key)).findFirst().map(LABELS::get).orElse("Ready for pickup");
            }
        };
    }
    public static void requireComplete(GroomingAppointment visit) {
        if (visit.getCompletedTasks() != null && !completed(visit).equals(required(visit)))
            throw new BadRequestException("Finish the visit checklist before completing this visit");
    }
}
