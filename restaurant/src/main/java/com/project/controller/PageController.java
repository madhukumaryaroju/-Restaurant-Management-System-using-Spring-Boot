package com.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.service.CategoryService;
import com.project.service.ProductService;
@Controller
public class PageController {
	@Autowired
	private CategoryService cservice;
	@Autowired
	private ProductService pservice;
	
	@GetMapping("/shop")
	public String shop(Model model) {
		model.addAttribute("categories",cservice.getAll());
		model.addAttribute("products",pservice.getAll());
		return "shop";
	}
	@GetMapping("/shop/category/{id}")
	public String shopByCategory(Model model,@PathVariable("id") int id) {
		model.addAttribute("categories",cservice.getAll());
		model.addAttribute("products",pservice.getProByCatId(id));
		return "shop";
	}
	@GetMapping("/shop/viewproduct/{id}")
	public String viewproduct(Model model,@PathVariable("id") int id) {
		model.addAttribute("product",pservice.fetchbyId(id).get());
		return "viewProduct";
	}
	@GetMapping("/userform")
	public String form() {
		return "userform";
	}
	@GetMapping("/user-login")
    public String userLogin() {
        return "user_login"; // This will return the user_login.html page
    }
}