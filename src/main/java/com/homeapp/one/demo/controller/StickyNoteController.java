package com.homeapp.one.demo.controller;

import java.util.List;

import com.homeapp.one.demo.models.StickyNote;
import com.homeapp.one.demo.services.StickyNoteService;
import org.apache.logging.log4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("StickyNotes/")
@CrossOrigin(origins = "http://localhost:3000")
public class StickyNoteController {

    private static Logger LOGGER = LogManager.getLogger(StickyNoteController.class);

    @Autowired
    private StickyNoteService stickyNoteService;

    @GetMapping("GetAll")
    public ResponseEntity<List<StickyNote>> getStickyNotes() {
        LOGGER.info("Getting all Sticky Notes, GetAll API");
        List<StickyNote> list = stickyNoteService.retrieveAllNotes();
        return new ResponseEntity<>(list, HttpStatus.ACCEPTED);
    }

    @PostMapping("AddNote")
    public ResponseEntity<List> addStickyNote(@RequestBody StickyNote note) {
        LOGGER.info("Adding new Sticky Note, AddNote API");
        stickyNoteService.create(note);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("DeleteNote/{id}")
    public ResponseEntity<List> deleteStickyNote(@PathVariable(value = "id") Long stickyNoteId) {
        LOGGER.info("Deleting Sticky Note, Delete Note API");
        StickyNote note = stickyNoteService.retrieveById(stickyNoteId).get();
        stickyNoteService.deleteNote(note);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("DeleteAllNotes")
    public ResponseEntity<List> deleteAllNotes() {
        LOGGER.info("Deleting ALL Sticky Notes, Delete Note API");
        stickyNoteService.deleteAll();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("EditNote")
    public ResponseEntity<List> editStickyNote(@RequestBody StickyNote note) {
        LOGGER.info("Editing Sticky Note, Edit Note API");
        stickyNoteService.editStickyNote(note);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}