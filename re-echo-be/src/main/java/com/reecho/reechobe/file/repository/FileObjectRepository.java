package com.reecho.reechobe.file.repository;

import com.reecho.reechobe.file.domain.FileObject;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileObjectRepository extends JpaRepository<FileObject, UUID> {
}
