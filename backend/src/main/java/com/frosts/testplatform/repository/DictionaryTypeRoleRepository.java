package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryTypeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryTypeRoleRepository extends JpaRepository<DictionaryTypeRole, Long> {

    List<DictionaryTypeRole> findByTypeId(Long typeId);

    List<DictionaryTypeRole> findByRoleId(Long roleId);

    Optional<DictionaryTypeRole> findByTypeIdAndRoleId(Long typeId, Long roleId);

    void deleteByTypeId(Long typeId);
}
