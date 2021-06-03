package com.baiyi.caesar.packer.sys;

import com.baiyi.caesar.common.util.BeanCopierUtil;
import com.baiyi.caesar.domain.generator.caesar.AuthRoleMenu;
import com.baiyi.caesar.domain.generator.caesar.Menu;
import com.baiyi.caesar.domain.generator.caesar.MenuChild;
import com.baiyi.caesar.domain.vo.common.TreeVO;
import com.baiyi.caesar.domain.vo.sys.MenuVO;
import com.baiyi.caesar.service.auth.AuthRoleMenuService;
import com.baiyi.caesar.service.sys.MenuChildService;
import com.baiyi.caesar.service.sys.MenuService;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author <a href="mailto:xiuyuan@xinc818.group">修远</a>
 * @Date 2021/6/2 10:53 上午
 * @Since 1.0
 */

@Component
public class MenuPacker {

    @Resource
    private MenuService menuService;

    @Resource
    private MenuChildService menuChildService;

    @Resource
    private AuthRoleMenuService authRoleMenuService;

    public List<Menu> toDOList(List<MenuVO.Menu> menuList) {
        return BeanCopierUtil.copyListProperties(menuList, Menu.class);
    }

    public List<MenuChild> toChildDOList(List<MenuVO.MenuChild> menuChildren) {
        return BeanCopierUtil.copyListProperties(menuChildren, MenuChild.class);
    }

    public List<MenuVO.Menu> toVOList(List<Menu> menuList) {
        return BeanCopierUtil.copyListProperties(menuList, MenuVO.Menu.class);
    }

    public List<MenuVO.MenuChild> toChildVOList(List<MenuChild> menuChildList) {
        return BeanCopierUtil.copyListProperties(menuChildList, MenuVO.MenuChild.class);
    }

    public List<TreeVO.Tree> wrapTree() {
        List<Menu> menuList = menuService.queryAllBySeq();
        List<TreeVO.Tree> treeList = Lists.newArrayListWithCapacity(menuList.size());
        menuList.forEach(ocMenu -> treeList.add(buildTree(ocMenu)));
        return treeList;
    }

    private TreeVO.Tree buildTree(Menu menu) {
        List<MenuChild> menuChildren = menuChildService.listByMenuId(menu.getId());
        List<TreeVO.Tree> treeList = Lists.newArrayListWithCapacity(menuChildren.size());
        menuChildren.forEach(menuChild -> treeList.add(buildTree(menuChild)));
        return TreeVO.Tree.builder()
                .label(menu.getTitle())
                .value(menu.getId() * -1)
                .children(treeList)
                .build();
    }

    private TreeVO.Tree buildTree(MenuChild menuChild) {
        return TreeVO.Tree.builder()
                .label(menuChild.getTitle())
                .value(menuChild.getId())
                .build();
    }


    public List<MenuVO.Menu> toVOList(Integer roleId) {
        List<AuthRoleMenu> authRoleMenuList = authRoleMenuService.listByRoleId(roleId);
        if (CollectionUtils.isEmpty(authRoleMenuList))
            return Collections.emptyList();
        List<MenuChild> menuChildren = querySubmenu(authRoleMenuList);
        return wrapVOList(menuChildren);
    }

    private List<MenuVO.Menu> wrapVOList(List<MenuChild> menuChildren) {
        Map<Integer, List<MenuChild>> map = menuChildren.stream()
                .collect(Collectors.groupingBy(MenuChild::getMenuId));
        List<MenuVO.Menu> menuList = Lists.newArrayListWithCapacity(map.size());
        map.forEach((k, y) -> {
            List<MenuVO.MenuChild> sort = toChildVOList(y.stream()
                    .sorted(Comparator.comparing(MenuChild::getSeq))
                    .collect(Collectors.toList()));
            Menu menu = menuService.getById(k);
            MenuVO.Menu menuVO = MenuVO.Menu.builder()
                    .title(menu.getTitle())
                    .icon(menu.getIcon())
                    .seq(menu.getSeq())
                    .menuChildren(sort)
                    .build();
            menuList.add(menuVO);
        });
        return menuList.stream()
                .sorted(Comparator.comparing(MenuVO.Menu::getSeq))
                .collect(Collectors.toList());
    }

    private List<MenuChild> querySubmenu(List<AuthRoleMenu> authRoleMenuList) {
        List<Integer> idList = authRoleMenuList.stream().map(AuthRoleMenu::getMenuChildId).collect(Collectors.toList());
        return menuChildService.listByIdList(idList);
    }
}

