package com.pawcare.demo.repository;
import com.pawcare.demo.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContactMessageRepository extends JpaRepository<ContactMessage,Long> {}

