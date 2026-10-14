CREATE TABLE `board_temp_images` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `board_id` BIGINT NOT NULL,
  `file_id` VARCHAR(100) NOT NULL,
  `uploader_email` VARCHAR(255) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_board_temp_images_file` (`file_id`),
  KEY `idx_board_temp_images_board` (`board_id`),
  CONSTRAINT `board_temp_images_board_fk` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_temp_images_user_fk` FOREIGN KEY (`uploader_email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
