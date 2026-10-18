const groomingPhotos = {
    "Basic Bath": {
        "src": "/images/grooming-services/Basic_Bath.png",
        "alt": "Basic Bath pet grooming photo"
    },
    "Full Grooming": {
        "src": "/images/grooming-services/Full_Grooming.png",
        "alt": "Full Grooming pet grooming photo"
    },
    "Nail Trimming": {
        "src": "/images/grooming-services/Nail_Trimming.png",
        "alt": "Nail Trimming pet grooming photo"
    },
    "Haircut": {
        "src": "/images/grooming-services/Haircut.png",
        "alt": "Haircut pet grooming photo"
    },
    "Ear Cleaning": {
        "src": "/images/grooming-services/Ear_Cleaning.png",
        "alt": "Ear Cleaning pet grooming photo"
    },
    "Teeth Cleaning": {
        "src": "/images/grooming-services/Teeth_Cleaning.png",
        "alt": "Teeth Cleaning pet grooming photo"
    },
    "Hygiene Trim": {
        "src": "/images/grooming-services/Hygiene_Trim.png",
        "alt": "Hygiene Trim pet grooming photo"
    },
    "De-shedding Treatment": {
        "src": "/images/grooming-services/De_shedding_Treatment.png",
        "alt": "De-shedding Treatment pet grooming photo"
    },
    "Flea & Tick Treatment": {
        "src": "/images/grooming-services/Flea_and_Tick_Treatment.png",
        "alt": "Flea & Tick Treatment pet grooming photo"
    }
};
function groomingServiceImage(service) {
    return service.image || groomingPhotos[service.name]?.src || '/images/grooming.png';
}
