package com.alan.alanapiinterface.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 接口信息（对应数据库 interface_info 表，接口服务侧只读，用于按请求路径识别被调用的接口）
 *
 * @author alan
 */
@TableName(value = "interface_info")
@Data
public class InterfaceInfo implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 接口地址
     */
    private String url;

    /**
     * 请求类型（GET/POST）
     */
    private String method;

    /**
     * 接口状态（0-关闭，1-开启）
     */
    private Integer status;

    /**
     * 是否删除(0-未删, 1-已删)
     */
    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
