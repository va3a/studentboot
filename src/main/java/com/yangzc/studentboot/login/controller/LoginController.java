package com.yangzc.studentboot.login.controller;

import com.yangzc.studentboot.common.annotation.Log;
import com.yangzc.studentboot.common.config.StudentBootConfig;
import com.yangzc.studentboot.common.controller.BaseController;
import com.yangzc.studentboot.common.domain.FileDO;
import com.yangzc.studentboot.common.domain.Tree;
import com.yangzc.studentboot.common.service.FileService;
import com.yangzc.studentboot.common.utils.*;
import com.yangzc.studentboot.system.domain.MenuDO;
import com.yangzc.studentboot.system.service.MenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Api(tags = {"登录接口"}, description = "登录接口")
@Controller
public class LoginController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    StudentBootConfig studentBootConfig;

    @Autowired
    MenuService menuService;

    @Autowired
    FileService fileService;

    @GetMapping({"/", ""})
    String welcome(Model model) {
        return "redirect:/blog";
    }

    @GetMapping({"/index"})
    String index(Model model) {
        List<Tree<MenuDO>> menus = menuService.listMenuTree(getUserId());
        model.addAttribute("menus", menus);
        model.addAttribute("name", getUser().getName());
        FileDO fileDO = fileService.get(getUser().getPicId());
        if (fileDO != null && fileDO.getUrl() != null) {
            if (fileService.isExist(fileDO.getUrl())) {
                model.addAttribute("picUrl", fileDO.getUrl());
            } else {
                model.addAttribute("picUrl", "/img/photo_s.jpg");
            }
        } else {
            model.addAttribute("picUrl", "/img/photo_s.jpg");
        }
        model.addAttribute("username", getUser().getUsername());
        return "index";
    }

    @GetMapping("/login")
    String login(Model model) {
        model.addAttribute("username", studentBootConfig.getUsername());
        model.addAttribute("password", studentBootConfig.getPassword());
        return "login";
    }

    @Log("登录")
    @ApiOperation(value = "登录", notes = "登录")
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    R ajaxLogin(String username, String password, String verify, HttpServletRequest request) {
        try {
            //从session中获取随机数
            String random = (String) request.getSession().getAttribute(RandomValidateCodeUtil.RANDOMCODEKEY);
            if (StringUtils.isBlank(verify)) {
                return R.error("请输入验证码");
            }
            if (random.equals(verify)) {
            } else {
                return R.error("请输入正确的验证码");
            }
        } catch (Exception e) {
            logger.error("验证码校验失败", e);
            return R.error("验证码校验失败");
        }
        password = MD5Utils.encrypt(username, password);
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
            return R.ok();
        } catch (AuthenticationException e) {
            return R.error("用户或密码错误");
        }
    }

    @ApiOperation(value = "退出", notes = "退出")
    @GetMapping("/logout")
    String logout() {
        ShiroUtils.logout();
        return "redirect:/login";
    }

    @GetMapping("/main")
    String main() {
        return "main";
    }

    /**
     * 生成验证码
     */
    @ApiOperation(value = "验证码", notes = "验证码图片")
    @GetMapping(value = "/getVerify", produces = MediaType.IMAGE_JPEG_VALUE)
    public void getVerify(HttpServletRequest request, HttpServletResponse response) {
        try {
            //Thread.sleep(500);
            response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
            response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expire", 0);
            RandomValidateCodeUtil randomValidateCode = new RandomValidateCodeUtil();
            randomValidateCode.getRandcode(request, response);//输出验证码图片方法
        } catch (Exception e) {
            logger.error("获取验证码失败>>>> ", e);
        }
    }

}
