package com.frosts.testplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_dictionary_type_role")
public class DictionaryTypeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(nullable = false, length = 20)
    private String permission;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
