package com.frosts.testplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_dictionary_log")
public class DictionaryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(name = "operated_at")
    private LocalDateTime operatedAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
