package com.d_tech.libsys.domain.model;

import jakarta.persistence.*;
import lombok.*;

// Book sınıfı, veritabanında "books" adlı tabloya karşılık gelir


// @Entity: Book sınıfının veritabanında bir tabloya karşılık geldiğini belirtir.
//@Id & @GeneratedValue: id alanı otomatik artan birincil anahtardır.
//@Column(nullable = false): Kitap başlığı boş bırakılamaz.
//Lombok Anotasyonları (@Data, @Builder, vb.) sayesinde getter, setter, constructor gibi kodlar otomatik oluşturulur.

@Entity
@Data  // Getter, Setter, toString, equals, hashCode otomatik oluşturulur
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID otomatik artar
    private Long id;

    @Column(nullable = false)  // Title alanı boş bırakılamaz
    private String title;

    private String author;  // Kitabın Yazarı

    private int year;  // Yayın yılı
}
