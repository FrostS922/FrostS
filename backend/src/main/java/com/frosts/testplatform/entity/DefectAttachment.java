package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "defect_attachment")
public class DefectAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_id")
    private Defect defect;

    @Column(nullable = false, length = 200)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(length = 50)
    private String fileType;

    private Long fileSize;

    @Column(length = 50)
    private String uploadedBy;
}
