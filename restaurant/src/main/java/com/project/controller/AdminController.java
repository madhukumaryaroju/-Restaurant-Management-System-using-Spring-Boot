package com.project.controller;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.dto.ProductDt;
import com.project.entity.Admin;
import com.project.entity.Category;
import com.project.entity.Product;
import com.project.service.AdminService;
import com.project.service.CategoryService;
import com.project.service.ProductService;

@Controller
public class AdminController {

    @Autowired
    private CategoryService cservice;
    
    @Autowired
    private ProductService pservice;
    
    @Autowired
    private AdminService aservice;
    
    @Value("${app.upload.dir:${user.home}/productImages}")
    private String uploadDir;

    @GetMapping("/admin")
    public String admin() {
        return "login";
    }
    
    @GetMapping("/register")
    public String register() {
        return "register";
    }
    
    @PostMapping("/register")
    public String registerAdmin(@RequestParam("email") String email, 
                              @RequestParam("password") String password,
                              RedirectAttributes redirectAttributes) {
        try {
            Admin a = new Admin();
            a.setEmail(email);
            a.setPassword(password);
            aservice.save(a);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful!");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@RequestParam("email") String email,
                       @RequestParam("password") String password,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            Optional<Admin> admin = aservice.findByEmail(email);
            if (admin.isPresent() && admin.get().getPassword().equals(password)) {
                model.addAttribute("userobject", admin.get());
                return "admin";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid credentials");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Login error: " + e.getMessage());
            return "redirect:/login";
        }
    }
    
    @GetMapping("/admin/categories")
    public String categoryPage(Model model) {
        model.addAttribute("categories", cservice.getAll());
        return "categories";
    }

    @GetMapping("/admin/categories/add")
    public String addCategory(Model model) {
        model.addAttribute("category", new Category());
        return "categoriesAdd";
    }

    @PostMapping("/admin/categories/add")
    public String postAddCategory(@ModelAttribute("category") Category c, 
                                RedirectAttributes redirectAttributes) {
        try {
            cservice.saveCategory(c);
            redirectAttributes.addFlashAttribute("successMessage", "Category added successfully!");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding category: " + e.getMessage());
            return "redirect:/admin/categories/add";
        }
    }

    @GetMapping("/admin/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            cservice.deletebyId(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/update/{id}")
    public String updateCategory(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<Category> category = cservice.fetchbyId(id);
            if (category.isPresent()) {
                model.addAttribute("category", category.get());
                return "categoriesAdd";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating category: " + e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @GetMapping("/admin/products")
    public String productPage(Model model) {
        model.addAttribute("products", pservice.getAll());
        return "products";
    }

    @GetMapping("/admin/products/add")
    public String addProduct(Model model) {
        model.addAttribute("productDTO", new ProductDt());
        model.addAttribute("categories", cservice.getAll());
        return "productsAdd";
    }

    @PostMapping("/admin/products/add")
    public String postAddProduct(@ModelAttribute("productDTO") ProductDt p,
                                @RequestParam("productImage") MultipartFile file,
                                @RequestParam(value = "imgName", required = false) String imgName,
                                RedirectAttributes redirectAttributes) {
        try {
            Product pro = new Product();
            pro.setId(p.getId());
            pro.setName(p.getName());
            pro.setPrice(p.getPrice());
            pro.setDescription(p.getDescription());
            pro.setWeight(p.getWeight());
            pro.setCategory(cservice.fetchbyId(p.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found")));

            if (!file.isEmpty()) {
                // Validate file
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new RuntimeException("Only image files are allowed");
                }

                // Generate unique filename
                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String imageUUID = UUID.randomUUID().toString() + fileExtension;

                // Create upload directory if it doesn't exist
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Save file
                Path targetLocation = uploadPath.resolve(imageUUID);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                pro.setImageName(imageUUID);
            } else if (imgName != null && !imgName.isEmpty()) {
                pro.setImageName(imgName);
            } else {
                pro.setImageName("default.png");
            }

            pservice.saveProduct(pro);
            redirectAttributes.addFlashAttribute("successMessage", "Product saved successfully!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving product: " + e.getMessage());
            return "redirect:/admin/products/add";
        }
    }

    @GetMapping("/admin/product/delete/{id}")
    public String deleteProduct(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            // Optional: Delete the associated image file
            Optional<Product> productOptional = pservice.fetchbyId(id);
            if (productOptional.isPresent()) {
                String imageName = productOptional.get().getImageName();
                if (imageName != null && !imageName.equals("default.png")) {
                    Path imagePath = Paths.get(uploadDir).resolve(imageName);
                    Files.deleteIfExists(imagePath);
                }
            }
            
            pservice.deletebyId(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/product/update/{id}")
    public String updateProduct(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product pro = pservice.fetchbyId(id)
                            .orElseThrow(() -> new RuntimeException("Product not found"));
            
            ProductDt pdt = new ProductDt();
            pdt.setId(pro.getId());
            pdt.setName(pro.getName());
            pdt.setPrice(pro.getPrice());
            pdt.setWeight(pro.getWeight());
            pdt.setDescription(pro.getDescription());
            pdt.setCategoryId(pro.getCategory().getId());
            pdt.setImageName(pro.getImageName());
            
            model.addAttribute("categories", cservice.getAll());
            model.addAttribute("productDTO", pdt);
            return "productsAdd";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading product: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}