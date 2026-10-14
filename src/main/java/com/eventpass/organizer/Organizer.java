package com.eventpass.organizer;

import com.eventpass.event.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "organizers")
@Getter @Setter @NoArgsConstructor 
public class Organizer {

     @Id 
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

     @Column(nullable = false, length = 150, unique = true)
     private String name;

     @Column(length = 2000)
     private String description;

     @Column(length = 255)
     private String website;

     @Column(name = "contact_email", nullable = false)
     private String contactEmail;

     @Enumerated(EnumType.STRING)
     @Column(nullable = false, length = 20)
     private OrganizerStatus status = OrganizerStatus.PENDING;

     @OneToMany(mappedBy = "organizer")
     private List<Event> events = new ArrayList<>();

     

}
