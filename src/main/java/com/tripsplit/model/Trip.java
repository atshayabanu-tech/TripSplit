package com.tripsplit.model;
import java.time.Instant;
 import java.util.ArrayList;
 import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
@Entity @Table(name="trips")
public class Trip {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(nullable=false,length=160) private String name;
 @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @OneToMany(mappedBy="trip",cascade=CascadeType.ALL,orphanRemoval=true) private List<Participant> participants=new ArrayList<>();
 @OneToMany(mappedBy="trip",cascade=CascadeType.ALL,orphanRemoval=true) private List<Expense> expenses=new ArrayList<>();
 protected Trip(){} public Trip(String name){this.name=name;} public UUID getId(){return id;} public String getName(){return name;} public Instant getCreatedAt(){return createdAt;} public List<Participant> getParticipants(){return participants;} public List<Expense> getExpenses(){return expenses;}
}
