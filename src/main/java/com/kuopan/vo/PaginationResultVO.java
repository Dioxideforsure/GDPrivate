package com.kuopan.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
@Data
public class PaginationResultVO<T> {
    private Integer totalCount;
    private Integer pageSize;
    private Integer pageNo;
    private Integer pageTotal;
    private List<T> list;

    public static <T> PaginationResultVO<T> convert2PaginationVO(IPage<T> page) {
        PaginationResultVO<T> result = new PaginationResultVO<>();
        result.setList(page.getRecords())
                .setTotalCount((int) page.getTotal())
                .setPageSize((int) page.getSize())
                .setPageNo((int) page.getCurrent())
                .setPageTotal((int) page.getPages());
        return result;
    }

}