package cn.xeblog.commons.entity;

import cn.xeblog.commons.enums.Game;
import lombok.Data;

import java.io.Serializable;

/**
 * @author anlingyi
 * @date 2020/8/17
 */
@Data
public class GameDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String opponentId;

    private Game game;

}