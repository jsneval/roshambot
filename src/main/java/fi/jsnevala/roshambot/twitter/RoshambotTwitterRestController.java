package fi.jsnevala.roshambot.twitter;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/foo")
public class RoshambotTwitterRestController {

    @RequestMapping(method = RequestMethod.GET, value = "/bar", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<Object> bar() {
        Map<String, Object> value = new HashMap<>();
        value.put("test", "works");
        return new ResponseEntity<>(value, HttpStatus.OK);

    }
}
