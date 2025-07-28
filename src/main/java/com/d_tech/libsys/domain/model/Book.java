package com.d_tech.libsys.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String author;

    private int year;

    /**
     * 🚀 NEW: Kitap görseli URL'i
     * Frontend için kitap kapağı gösterilmesi için
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * 🚀 NEW: Kitap açıklaması
     * Opsiyonel - daha zengin içerik için
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 🚀 NEW: ISBN numarası
     * Kitap tanımlama için
     */
    @Column(name = "isbn")
    private String isbn;

    /**
     * 🚀 NEW: Sayfa sayısı
     */
    @Column(name = "page_count")
    private Integer pageCount;

    /**
     * 🚀 NEW: Kategori/Tür
     */
    @Column(name = "category")
    private String category;

    /**
     * 🚀 NEW: Yayınevi
     */
    @Column(name = "publisher")
    private String publisher;

    /**
     * 🚀 NEW: Dil
     */
    @Column(name = "language")
    @Builder.Default
    private String language = "Turkish";

    /**
     * 🚀 NEW: Default image URL generator
     * Eğer imageUrl null ise, placeholder görseli döndürür
     */
    public String getImageUrlOrDefault() {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl;
        }
        // Placeholder image - Unsplash'den güzel kitap görselleri
        return generatePlaceholderImage();
    }

    /**
     * 🚀 NEW: Placeholder image generator
     * Her kitap için farklı renkte placeholder
     */
    private String generatePlaceholderImage() {
        // Hash'e göre farklı renkler üret
        int hash = Math.abs((title + author).hashCode());
        String[] colors = {"3b82f6", "ef4444", "10b981", "f59e0b", "8b5cf6", "06b6d4", "84cc16", "f97316"};
        String color = colors[hash % colors.length];

        // Unsplash ile kitap temalı placeholder
        return String.format("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&h=400&fit=crop&auto=format&q=80&ixlib=rb-4.0.3&overlay-color=%s&overlay-blend=multiply&overlay-opacity=30", color);
    }

    /**
     * 🚀 NEW: Short description for cards
     */
    public String getShortDescription() {
        if (description == null || description.length() <= 150) {
            return description;
        }
        return description.substring(0, 147) + "...";
    }

    /**
     * 🚀 NEW: Full title with author
     */
    public String getFullTitle() {
        return title + (author != null ? " - " + author : "");
    }

    /**
     * 🚀 NEW: Display year or "Unknown"
     */
    public String getDisplayYear() {
        return year > 0 ? String.valueOf(year) : "Bilinmeyen";
    }
}