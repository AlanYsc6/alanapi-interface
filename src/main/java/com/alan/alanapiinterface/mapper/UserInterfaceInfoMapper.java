package com.alan.alanapiinterface.mapper;

import com.alan.alanapiinterface.model.entity.UserInterfaceInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户调用接口关系数据库操作
 *
 * @author alan
 */
public interface UserInterfaceInfoMapper extends BaseMapper<UserInterfaceInfo> {

    /**
     * 原子计数：总次数 +1、剩余次数 -1
     * <p>
     * 条件更新保证并发下不会把剩余次数扣成负数：
     * 恰好剩余 1 次时并发请求只有一条 UPDATE 能命中 leftNum > 0，其余返回 0
     *
     * @param userId          调用用户 id
     * @param interfaceInfoId 接口 id
     * @return 命中的行数，0 表示无可用次数 / 记录被禁用或不存在
     */
    @Update("UPDATE user_interface_info SET totalNum = totalNum + 1, leftNum = leftNum - 1 "
            + "WHERE userId = #{userId} AND interfaceInfoId = #{interfaceInfoId} "
            + "AND status = 0 AND leftNum > 0 AND isDelete = 0")
    int countOnce(@Param("userId") long userId, @Param("interfaceInfoId") long interfaceInfoId);
}
