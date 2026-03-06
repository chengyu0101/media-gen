package com.cy.mediagen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.mediagen.entity.AssetLibrary;
import com.cy.mediagen.mapper.AssetLibraryMapper;
import com.cy.mediagen.service.AssetLibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数字资产库服务实现类
 *
 * @author cy
 * @date 2026-03-06
 */
@Slf4j
@Service
public class AssetLibraryServiceImpl extends ServiceImpl<AssetLibraryMapper, AssetLibrary>
        implements AssetLibraryService {
}
