package com.example.project.controllers;

import com.example.project.models.Client;
import com.example.project.models.Item;
import com.example.project.models.Users;
import com.example.project.security.UsersDetails;
import com.example.project.service.ClientService;
import com.example.project.service.ItemService;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/client")
public class ClientController {
    private Client client;
    private final ClientService clientService;
    private final ItemService itemService;

    @Value("${file.images.viewPath}")
    private String viewPath;

    @Value("${file.images.uploadPath}")
    private String uploadPath;

    @Value("${file.images.defaultImage}")
    private String defaultPicture;

    @Autowired
    public ClientController(ClientService clientService, ItemService itemService) {
        this.clientService = clientService;
        this.itemService = itemService;
    }

    @GetMapping()
    public String home(Model model){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            UsersDetails user = (UsersDetails) authentication.getPrincipal();
            Users users = user.getUser();

        client = clientService.findByUserId(users.getId());
        List<Item> itemList = itemService.findLast();

        model.addAttribute("client", client);
        model.addAttribute("itemList", itemList);

        return "client/index";
    }

    @GetMapping("/shopping")
    public String shopping(Model model){
        List<Item> itemList = itemService.findAll();

        model.addAttribute("itemList", itemList);

        return "client/shopping";
    }

    @PreAuthorize("hasAnyRole('CLIENT')")
    @GetMapping("/my-items")
    public String myItems(Model model){
        List<Item> itemList = itemService.findByClientId(client.getId());

        model.addAttribute("itemList", itemList);

        return "client/shopping";
    }

    @PreAuthorize("hasAnyRole('CLIENT')")
    @GetMapping("/new-item")
    public String newItem(@ModelAttribute(name = "item") Item item){
        return "client/new-item";
    }

    @PostMapping("/new-item")
    public String postItem(@ModelAttribute(name = "item") Item item,
                           @RequestParam(value = "picture") MultipartFile image){

        item.setOwner(client);
        item = itemService.save(item);

        if(!image.isEmpty()){
            try {

                String pickName = DigestUtils.sha1Hex(item.getName() + "?item_id=" + item.getId());
                byte[] bytes = image.getBytes();
                Path path = Paths.get(uploadPath + pickName + ".jpg");
                Files.write(path, bytes);

                item.setImage(pickName);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        itemService.save(item);


        return "redirect:/client";
    }

    @GetMapping(value = "/viewphoto/{url}", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] viewItemPhoto(@PathVariable(name = "url") String url) throws IOException {
        String pictureUrl = "";
        if (url != null) {
            pictureUrl = viewPath + url + ".jpg";
        }

        InputStream in;

        try {
            ClassPathResource resource = new ClassPathResource(pictureUrl);
            in = resource.getInputStream();
        }catch (Exception e){
            ClassPathResource resource = new ClassPathResource(viewPath + defaultPicture);
            in = resource.getInputStream();
            e.printStackTrace();
        }

        return IOUtils.toByteArray(in);
    }

    @GetMapping("/details/{id}")
    public String itemInfo(Model model,
                           @PathVariable(name = "id")Integer id){

        Item item = itemService.findById(id);

        model.addAttribute("item", item);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UsersDetails user = (UsersDetails) authentication.getPrincipal();
        Users users = user.getUser();

        if(users.getRole().equalsIgnoreCase("ROLE_ADMIN") || Objects.equals(item.getOwner().getUser().getId(), users.getId())){
            model.addAttribute("owner", true);
        }

        return "client/item-desc";
    }

}
