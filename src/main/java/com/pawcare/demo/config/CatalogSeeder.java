package com.pawcare.demo.config;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Component
@RequiredArgsConstructor
public class CatalogSeeder implements CommandLineRunner {
    private final SpeciesRepository speciesRepo;
    private final BreedRepository breedRepo;
    private final ProductCategoryRepository categoryRepo;
    private final ProductRepository productRepo;
    private final GroomingServiceRepository groomingRepo;
    @Override @Transactional
    public void run(String... args) {
        String[][] species = {
            {"Dog","Labrador Retriever","German Shepherd","Golden Retriever","Bulldog","Poodle","Beagle","Mixed Breed"},
            {"Cat","Persian","Siamese","Maine Coon","British Shorthair","Domestic Shorthair","Mixed Breed"},
            {"Bird","Cockatiel","Budgerigar","Parrot","Canary"},
            {"Rabbit","Holland Lop","Netherland Dwarf","Mixed Breed"},
            {"Fish","Goldfish","Guppy","Betta","Koi"},
            {"Hamster","Syrian","Dwarf"}
        };
        for (String[] row : species) {
            Species sp = speciesRepo.findAll().stream().filter(s->s.getName().equalsIgnoreCase(row[0])).findFirst()
                .orElseGet(()->speciesRepo.save(Species.builder().name(row[0]).description("Pet " + row[0].toLowerCase()).build()));
            for(int i=1;i<row.length;i++) {
                String name=row[i];
                if (breedRepo.findBySpeciesId(sp.getId()).stream().noneMatch(b->b.getName().equalsIgnoreCase(name)))
                    breedRepo.save(Breed.builder().name(name).species(sp).build());
            }
        }
        String[][] products = {
            {"PC-DOG-FOOD","Adult Dog Food","Dog Food","6500.00","dog-food","Balanced dry food for adult dogs. 2 kg bag."},
            {"PC-CAT-FOOD","Adult Cat Food","Cat Food","4200.00","cat-food","Complete dry food for adult cats. 1 kg bag."},
            {"PC-ROPE","Cotton Rope Toy","Toys","900.00","rope","A knotted cotton rope for supervised play."},
            {"PC-BALL","Rubber Play Ball","Toys","750.00","ball","Textured rubber ball for fetching and play."},
            {"PC-COLLAR","Adjustable Collar","Accessories","1500.00","collar","Adjustable collar with a secure buckle."},
            {"PC-BOWL","Stainless Steel Bowl","Accessories","1200.00","bowl","Easy-clean feeding bowl with a non-slip base."},
            {"PC-SHAMPOO","Gentle Pet Shampoo","Grooming","1800.00","shampoo","Gentle cleansing shampoo, 250 ml."},
            {"PC-BED","Cozy Pet Bed","Bedding","6500.00","bed","Soft, washable cushion bed for everyday comfort."}
        };
        for(String[] row: products) {
            ProductCategory cat=categoryRepo.findAll().stream().filter(c->c.getName().equalsIgnoreCase(row[2])).findFirst()
                .orElseGet(()->categoryRepo.save(ProductCategory.builder().name(row[2]).isActive(true).build()));
            var existing=productRepo.findBySku(row[0]);
            if(existing.isEmpty())
                productRepo.save(Product.builder().sku(row[0]).name(row[1]).category(cat).price(new BigDecimal(row[3]))
                    .image("/images/products/"+row[4]+".png").description(row[5]).brand("PawCare")
                    .stockQuantity(30).isActive(true).isFeatured(true).build());
            // Existing products keep the owner's saved prices, stock, details and image.
        }
        // Sri Lankan market-aligned grooming prices (LKR). These are intentionally stored as normal
        // catalogue prices so owners can combine individual services in one appointment.
        String[][] services={
            {"Basic Bath","2800.00","45","Shampoo bath, blow dry and brushing."},
            {"Full Grooming","6000.00","90","Full-body trim with bath, brushing and finishing care."},
            {"Nail Trimming","500.00","15","Careful nail clipping and paw check."},
            {"Ear Cleaning","500.00","15","Gentle external ear cleaning."},
            {"Teeth Cleaning","450.00","15","Basic teeth cleaning and mouth freshening."},
            {"Hygiene Trim","1500.00","30","Sanitary, paw-pad and eye-area hygiene trim."},
            {"De-shedding Treatment","1800.00","45","Specialized de-shedding shampoo and brush-out."},
            {"Flea & Tick Treatment","2200.00","45","Medicated bath and coat care for flea/tick treatment."}
        };
        java.util.List<GroomingService> existingServices=groomingRepo.findAll();
        for(String[] row:services) {
            GroomingService service=existingServices.stream().filter(g->g.getName().equalsIgnoreCase(row[0])).findFirst().orElse(null);
            if(service!=null) continue;
            service=GroomingService.builder().name(row[0]).isActive(true).build();
            service.setDescription(row[3]);
            service.setPrice(new BigDecimal(row[1]));
            service.setDurationMinutes(Integer.valueOf(row[2]));
            service.setImage(defaultGroomingImage(row[0]));
            groomingRepo.save(service);
        }
    }
    private String defaultGroomingImage(String name) {
        String file=switch(name) {
            case "De-shedding Treatment" -> "De_shedding_Treatment";
            case "Flea & Tick Treatment" -> "Flea_and_Tick_Treatment";
            default -> name.replace(' ', '_');
        };
        return "/images/grooming-services/"+file+".png";
    }
}

