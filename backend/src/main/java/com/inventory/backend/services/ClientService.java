package com.inventory.backend.services;

import com.inventory.backend.model.Client;
import com.inventory.backend.model.Product;
import com.inventory.backend.repository.ClientRepository;
import org.springframework.stereotype.Service;
import java.util.List;

import javax.management.RuntimeErrorException;

@Service
public class ClientService{

  private final ClientRepository repo;
  
  public ClientService(ClientRepository repo){
    this.repo = repo;
  }


  // find all clients
  public List<Client> getAllClients(){
    return repo.findByDeletedFalseOrDeletedIsNull();
  }

    // Obtener todos los clientes incluyendo eliminados (para analytics)
  public List<Client> getAllClientsIncludingDeleted(){
    return repo.findAll();
  }

  public Client createClient(Client c) {
    if (c == null) {
      throw new IllegalArgumentException("Client cannot be null");
    }
    c.setDeleted(false); // Asegurar que clientes nuevos no estén eliminados
    return repo.save(c);
  }

  public Client getClientById(Long id) {
    return repo.findById(id).orElseThrow(() -> new RuntimeException("Client not found"));
  }

  public void deleteClientById(Long id) {
    if(repo.existsById(id)) {
      Client client = repo.findById(id).orElse(null);
      if (client != null) {
        client.setDeleted(true);
        repo.save(client);
      }
    }
  }

  public Client findMostLoyalClient(){
    return repo.findFirstByOrderBySpentAmountDesc();
  }

  public long getClientCount(){
    return repo.countByDeletedFalseOrDeletedIsNull();
  }

  
}

