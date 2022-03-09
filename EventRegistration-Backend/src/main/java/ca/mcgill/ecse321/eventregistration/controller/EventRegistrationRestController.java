package ca.mcgill.ecse321.eventregistration.controller;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.mcgill.ecse321.eventregistration.dto.EventDto;
import ca.mcgill.ecse321.eventregistration.dto.PersonDto;
import ca.mcgill.ecse321.eventregistration.dto.RegistrationDto;
import ca.mcgill.ecse321.eventregistration.model.Event;
import ca.mcgill.ecse321.eventregistration.model.Person;
import ca.mcgill.ecse321.eventregistration.model.Registration;
import ca.mcgill.ecse321.eventregistration.service.EventRegistrationService;

//when a page is searched eg registo.com, an http request is sent via URL to the domain which delivers back something
//can program a button to send a specific request 
//post is more secure
//when passing parameters pass at the end registo.com/persons.php?msg=hi&age=20/id when checking for format,
//everything after the ? is ignored

@CrossOrigin(origins = "*")
@RestController
public class EventRegistrationRestController {

	@Autowired
	private EventRegistrationService service;

	@GetMapping(value = { "/persons", "/persons/" })
	public List<PersonDto> getAllPersons() {
		//getAllPersons returns a list of all "Person", need to convert to DTO so that we can send to frontend
		return service.getAllPersons().stream().map(p -> convertToDto(p)).collect(Collectors.toList());
	}

	//registo.com/persons wont work since format doesnt match. must be registo.com/persons/jess for example
	//the name will be extracted
	@PostMapping(value = { "/persons/{name}", "/persons/{name}/" })
	public PersonDto createPerson(@PathVariable("name") String name) throws IllegalArgumentException {
		//service class centralizes the DAO methods
		Person person = service.createPerson(name);
		return convertToDto(person);
	}
	
	@PostMapping(value = { "/events/{name}", "/events/{name}/" })
	public EventDto createEvent(@PathVariable("name") String name, @RequestParam Date date,
	@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME, pattern = "HH:mm") LocalTime startTime,
	@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME, pattern = "HH:mm") LocalTime endTime)
	throws IllegalArgumentException {
		Event event = service.createEvent(name, date, Time.valueOf(startTime), Time.valueOf(endTime));
		return convertToDto(event);
	}

	@GetMapping(value = { "/events", "/events/" })
	public List<EventDto> getAllEvents() {
		List<EventDto> eventDtos = new ArrayList<>();
		for (Event event : service.getAllEvents()) {
			eventDtos.add(convertToDto(event));
		}
		return eventDtos;
	}

	@PostMapping(value = { "/register", "/register/" })
	public RegistrationDto registerPersonForEvent(@RequestParam(name = "person") PersonDto pDto,
		@RequestParam(name = "event") EventDto eDto) throws IllegalArgumentException {
		Person p = service.getPerson(pDto.getName());
		Event e = service.getEvent(eDto.getName());

		Registration r = service.register(p, e);
		return convertToDto(r, p, e);
	}

	
	@GetMapping(value = { "/registrations/person/{name}", "/registrations/person/{name}/" })
	public List<EventDto> getEventsOfPerson(@PathVariable("name") PersonDto pDto) {
		//pDto is extracted from the url (everything incoming with a request to the backend are DTOs) 	
		Person p = convertToDomainObject(pDto);
		return createEventDtosForPerson(p);
	}

	@GetMapping(value = { "/events/{name}", "/events/{name}/" })
	public EventDto getEventByName(@PathVariable("name") String name) throws IllegalArgumentException {
		return convertToDto(service.getEvent(name));
	}

	private EventDto convertToDto(Event e) {
		if (e == null) {
			throw new IllegalArgumentException("There is no such Event!");
		}
		EventDto eventDto = new EventDto(e.getName(),e.getDate(),e.getStartTime(),e.getEndTime());
		return eventDto;
	}

	private PersonDto convertToDto(Person p) {
		if (p == null) {
			throw new IllegalArgumentException("There is no such Person!");
		}
		PersonDto personDto = new PersonDto(p.getName());
		personDto.setEvents(createEventDtosForPerson(p));
		return personDto;
	}

	private RegistrationDto convertToDto(Registration r, Person p, Event e) {
		EventDto eDto = convertToDto(e);
		PersonDto pDto = convertToDto(p);
		return new RegistrationDto(pDto, eDto);
	}

	private Person convertToDomainObject(PersonDto pDto) {
		List<Person> allPersons = service.getAllPersons();
		for (Person person : allPersons) {
			if (person.getName().equals(pDto.getName())) {
				return person;
			}
		}
		return null;
	}

	private List<EventDto> createEventDtosForPerson(Person p) {
		List<Event> eventsForPerson = service.getEventsAttendedByPerson(p);
		List<EventDto> events = new ArrayList<>();
		for (Event event : eventsForPerson) {
			events.add(convertToDto(event));
		}
		return events;
	}
}