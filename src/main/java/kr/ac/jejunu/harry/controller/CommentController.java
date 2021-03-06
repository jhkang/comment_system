package kr.ac.jejunu.harry.controller;

import kr.ac.jejunu.harry.annotation.auth.AuthorizationRequired;
import kr.ac.jejunu.harry.exception.BadRequestException;
import kr.ac.jejunu.harry.exception.SessionAccountNotFoundException;
import kr.ac.jejunu.harry.model.Comment;
import kr.ac.jejunu.harry.model.Opinion;
import kr.ac.jejunu.harry.model.User;
import kr.ac.jejunu.harry.repository.CommentRepository;
import kr.ac.jejunu.harry.repository.OpinionRepository;
import kr.ac.jejunu.harry.repository.UserRepository;
import kr.ac.jejunu.harry.response.ResponseBuilder;
import kr.ac.jejunu.harry.response.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by jhkang on 2016-06-12.
 */
@Controller
@RequestMapping(path = "/comments")
public class CommentController {
    @Autowired
    CommentRepository commentRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    OpinionRepository opinionRepository;

    @RequestMapping(path = "/{id:[0-9]+}", method = RequestMethod.GET)
    public void get(@PathVariable Integer id, HttpServletRequest request, Model model) {
        ResponseBuilder builder = new ResponseBuilder(request);
        Comment comment = commentRepository.findOne(id);
        if(comment == null) {
            builder.setStatusCode(ResponseCode.CONFLICT, "Comment Not Found");
        } else {
            builder.addAttribute(comment);
        }
        builder.buildWithModel(model);
    }

    @RequestMapping(method = RequestMethod.GET)
    public void list(@RequestParam(required = false, defaultValue = "1") Integer page,
                     @RequestParam(required = false, defaultValue = "15") Integer size,
                     HttpServletRequest request, Model model) {
        ResponseBuilder builder = new ResponseBuilder(request);
        Page<Comment> comments = commentRepository
                .findAll(new PageRequest(page - 1, size, Sort.Direction.DESC, "createdAt"));
        builder.addAttribute("totalPage", comments.getTotalPages());
        builder.addAttribute("size", comments.getSize());
        builder.addAttribute("page", comments.getNumber() + 1);
        builder.addAttribute("first", comments.isFirst());
        builder.addAttribute("last", comments.isLast());
        builder.addAttribute("comments", comments.getContent());
        builder.buildWithModel(model);
    }

    @RequestMapping(method = RequestMethod.POST)
    @AuthorizationRequired
    public void save(@RequestPayload String commentStr, HttpServletRequest request, Model model)
            throws MissingServletRequestParameterException {
        if(commentStr == null) {
            throw new MissingServletRequestParameterException("commentStr", String.class.getTypeName());
        }
        User user = getCurrentSessionUser(request);

        if(commentStr.getBytes().length > 255 || commentStr.getBytes().length < 1) {
            throw new BadRequestException("Comment length is must be more then 1, less then 255");
        }
        Comment comment = new Comment();
        comment.setComment(commentStr);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        ResponseBuilder builder = new ResponseBuilder(request);
        builder.addAttribute(savedComment);
        builder.buildWithModel(model);
    }

    @RequestMapping(path = "/{id:[0-9]+}", method = RequestMethod.DELETE)
    @AuthorizationRequired
    public void delete(@PathVariable Integer id, HttpServletRequest request, Model model) {
        Comment comment = commentRepository.findOne(id);
        User user = getCurrentSessionUser(request);
        ResponseBuilder builder = new ResponseBuilder(request);
        if(comment == null || comment.getCid() < 0) {
            builder.setStatusCode(ResponseCode.CONFLICT, "Comment Not Found");
            builder.buildWithModel(model);
            return;
        }

        if(user.getUid() != comment.getUser().getUid()) {
            builder.setStatusCode(ResponseCode.CONFLICT, "Not Matched Comment Author");
            builder.buildWithModel(model);
            return;
        }

        List<Opinion> opinions = opinionRepository.findAllByComment(comment);
        opinionRepository.delete(opinions);

        commentRepository.delete(comment.getCid());
        builder.addAttribute("cid", comment.getCid());
        builder.buildWithModel(model);
    }

    @RequestMapping(path = "/{id:[0-9]+}/like", method = RequestMethod.GET)
    @AuthorizationRequired
    public void like(@PathVariable Integer id, HttpServletRequest request, Model model) {
        Comment comment = commentRepository.findOne(id);
        User user = getCurrentSessionUser(request);
        ResponseBuilder builder = new ResponseBuilder(request);
        if(comment == null || comment.getCid() < 0) {
            builder.setStatusCode(ResponseCode.CONFLICT, "Comment Not Found");
            builder.buildWithModel(model);
            return;
        }
        Opinion opinion = new Opinion();
        opinion.setUser(user);
        opinion.setComment(comment);
        opinion.setType(Opinion.TYPE.LIKE);

        Opinion savedOpinion = opinionRepository.save(opinion);
        builder.addAttribute(savedOpinion);
        builder.buildWithModel(model);
    }

    @RequestMapping(path = "/{id:[0-9]+}/dislike", method = RequestMethod.GET)
    @AuthorizationRequired
    public void dislike(@PathVariable Integer id, HttpServletRequest request, Model model) {
        Comment comment = commentRepository.findOne(id);
        User user = getCurrentSessionUser(request);
        ResponseBuilder builder = new ResponseBuilder(request);
        if(comment == null || comment.getCid() < 0) {
            builder.setStatusCode(ResponseCode.CONFLICT, "Comment Not Found");
            builder.buildWithModel(model);
            return;
        }
        Opinion opinion = new Opinion();
        opinion.setUser(user);
        opinion.setComment(comment);
        opinion.setType(Opinion.TYPE.DISLIKE);

        Opinion savedOpinion = opinionRepository.save(opinion);
        builder.addAttribute(savedOpinion);
        builder.buildWithModel(model);
    }

    private User getCurrentSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if(session == null || session.getAttribute("uid") == null) {
            throw new SessionAccountNotFoundException();
        }
        Integer uid = (Integer) session.getAttribute("uid");
        User user = userRepository.findOne(uid);
        if(user == null || user.getUid() < 0) {
            throw new SessionAccountNotFoundException();
        }
        return user;
    }
}
