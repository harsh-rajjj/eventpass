package com.eventpass.user;
import com.eventpass.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.eventpass.organizer.Organizer;
import jakarta.persistence.Enumerated;


@Entity 
@Table(name = "users")
@Getter 
@Setter       
@NoArgsConstructor 
public class User extends BaseEntity {

     @Id 
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

     @Column(name = "full_name", nullable = false, length = 100)
     private String fullName;

     @Column(nullable = false, unique = true)
     private String email;

     @Column(name = "password_hash", nullable = false)
     private String passwordHash;

     @Enumerated(EnumType.STRING)
     @Column(nullable = false, length = 20)
     private Role role;

     @Column(nullable = false)
     private boolean enabled = true;

     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "organizer_id")
     private Organizer organizer;
}
