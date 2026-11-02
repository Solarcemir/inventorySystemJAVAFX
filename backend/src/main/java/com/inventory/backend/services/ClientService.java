package com.inventory.backend.services;

import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.Client;
import com.inventory.backend.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientService{

  private final ClientRepository repo;
  private final ActivityLogService logService;

  public ClientService(ClientRepository repo, ActivityLogService logService){
    this.repo = repo;
    this.logService = logService;
  }


  // find all clients
  public List<Client> getAllClients(){
    return repo.findByDeletedFalseOrDeletedIsNull();
  }

    // Obtener todos los clientes incluyendo eliminados (para analytics)
  public List<Client> getAllClientsIncludingDeleted(){
    return repo.findAll();
  }

  @Transactional
  public Client createClient(Client c) {
    if (c == null) {
      throw ApiException.badRequest("Client cannot be null");
    }
    validate(c);
    c.setId(null); // Un cliente nuevo nunca sobrescribe a uno existente
    c.setSpentAmount(BigDecimal.ZERO);
    c.setDeleted(false); // Asegurar que clientes nuevos no estén eliminados
    Client saved = repo.save(c);
    logService.log("ADD_CLIENT", "Added client \"" + fullName(saved) + "\"");
    return saved;
  }

  public Client getClientById(Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("Client not found"));
  }

  // El monto gastado lo calculan las ventas, nunca se edita a mano
  @Transactional
  public Client updateClient(Long id, Client details) {
    validate(details);
    Client client = getClientById(id);
    client.setFirstName(details.getFirstName());
    client.setLastName(details.getLastName());
    client.setEmail(details.getEmail());
    client.setPhoneNumber(details.getPhoneNumber());
    Client saved = repo.save(client);
    logService.log("EDIT_CLIENT", "Edited client \"" + fullName(saved) + "\" (ID: " + id + ")");
    return saved;
  }

  @Transactional
  public void deleteClientById(Long id) {
    repo.findById(id).ifPresent(client -> {
      client.setDeleted(true);
      repo.save(client);
      logService.log("DELETE_CLIENT", "Deleted client \"" + fullName(client) + "\" (ID: " + id + ")");
    });
  }

  public Client findMostLoyalClient(){
    return repo.findFirstByOrderBySpentAmountDesc();
  }

  public long getClientCount(){
    return repo.countByDeletedFalseOrDeletedIsNull();
  }

  private void validate(Client c) {
    if (c.getFirstName() == null || c.getFirstName().isBlank()
        || c.getLastName() == null || c.getLastName().isBlank()) {
      throw ApiException.badRequest("Nombre y apellido son obligatorios");
    }
  }

  private String fullName(Client c) {
    return c.getFirstName() + " " + c.getLastName();
  }
}
