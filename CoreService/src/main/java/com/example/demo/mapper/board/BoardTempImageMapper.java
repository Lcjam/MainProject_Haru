package com.example.demo.mapper.board;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BoardTempImageMapper {
    @Insert("INSERT INTO board_temp_images (board_id, file_id, uploader_email) VALUES (#{boardId}, #{fileId}, #{uploaderEmail})")
    int insert(@Param("boardId") Long boardId, @Param("fileId") String fileId,
               @Param("uploaderEmail") String uploaderEmail);

    @Select("SELECT uploader_email FROM board_temp_images WHERE board_id = #{boardId} AND file_id = #{fileId}")
    String findUploader(@Param("boardId") Long boardId, @Param("fileId") String fileId);

    @Delete("DELETE FROM board_temp_images WHERE board_id = #{boardId} AND file_id = #{fileId}")
    int delete(@Param("boardId") Long boardId, @Param("fileId") String fileId);
}
