package main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * Этот класс нужен для корректного обновления страницы Статистики
 */
@Controller
public class DefaultController {

@Autowired
private YAMLConfig myConfig;

@RequestMapping("/{pathToInterface}")
    public String index(@PathVariable String pathToInterface, Model model){
    String pathSlash = "/" + pathToInterface;
    String data = SiteController.getIsIndexing() ? "Stop Indexing" : "Start Indexing";
    String dataAltText = SiteController.getIsIndexing() ? "Start Indexing" : "Stop Indexing";
    String dataSend = SiteController.getIsIndexing() ? "stopIndexing" : "startIndexing";
    String dataAltSend = SiteController.getIsIndexing() ? "startIndexing" : "stopIndexing";
    model.addAttribute("dataCheck", SiteController.getIsIndexing());
    model.addAttribute("data", data);
    model.addAttribute("dataAltText", dataAltText);
    model.addAttribute("dataSend", dataSend);
    model.addAttribute("dataAltSend", dataAltSend);

     if(pathSlash.equals(myConfig.getPathToInterface())) {
         return "../static/index";
     } else {
         return "";
     }
    }
}
