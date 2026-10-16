package com.eventpass.common;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass 
@EntityListeners(AuditingEntityListener.class)
@Getter      
@Setter              
public abstract class BaseEntity {

     @CreatedDate 
     @Column(name = "created_at", nullable = false, updatable = false)
     private Instant createdAt;

     @CreatedBy 
     @Column(name = "created_by", nullable = false, updatable = false, length = 100)
     private String createdBy;

     @LastModifiedDate 
     @Column(name = "updated_at", nullable = false)
     private Instant updatedAt;

     @LastModifiedBy 
     @Column(name = "updated_by", nullable = false, length = 100)
     private String updatedBy;
}
