package com.inventory.backend.controller;

import com.inventory.backend.auth.AdminOnly;
import com.inventory.backend.model.Client;
import com.inventory.backend.services.ClientService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    @GetMapping("/{id}")
    public Client getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

    @PostMapping
    public Client createClient(@RequestBody Client client) {
        return clientService.createClient(client);
    }

    @AdminOnly
    @PutMapping("/{id}")
    public Client updateClient(@PathVariable Long id, @RequestBody Client client) {
        return clientService.updateClient(id, client);
    }

    @AdminOnly
    @DeleteMapping("/{id}")
    public void deleteClient(@PathVariable Long id) {
        clientService.deleteClientById(id);
    }

    @GetMapping("/most-loyal")
    public Client getMostLoyalClient() {
        return clientService.findMostLoyalClient();
    }


    @GetMapping("/count")
    public long getClientCount(){
        return clientService.getClientCount();
    }
}
