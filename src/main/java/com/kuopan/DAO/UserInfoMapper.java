package com.kuopan.DAO;

import com.kuopan.Entity.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * To put in User Information Mapper 接口
 * </p>
 *
 * @author Kuo
 * @since 2026-01-13
 */
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    // Apply the change from XML, when adding space or reducing space.
    int updateRemainSpaceByGUID(@Param("id") String id, @Param("occu_space") Long occuSpace);


}
