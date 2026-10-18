package com.pawcare.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class PetPassportPageController {
    @GetMapping("/p/{token}")
    public RedirectView openPassport(@PathVariable String token) {
        return new RedirectView("/pages/pet-passport.html?p=" + token);
    }
}
