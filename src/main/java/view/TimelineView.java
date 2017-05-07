package view;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import org.controlsfx.control.PopOver;
import interfaces.TimelineViewListener;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import model.Event;
import model.Timeline;
import model.Event.EventType;

/**
 * Class drawing the graphics for how a timeline will be displayed. This class
 * can be added as a component in a user interface.
 * 
 * @author Jesper Bergstrom and Zacky Kharboutli
 * @version 0.00.00
 * @name TimelineView.java
 */
public class TimelineView extends StackPane {

	private static final Dimension2D DEFAULT_SIZE = new Dimension2D(800, 400);

	private final int ROWS = 30;
	private int width = 50; // For time perspective
	private Pane[] panes = new Pane[ROWS];
	private StackPane stack = new StackPane();
	private ScrollPane scroll;
	private HBox dates;
	private VBox vbox;
	private Timeline currentTimeline;
	private Button addEventButton;
	private EventShape shape;
	PopOver hoverOver = new PopOver();
	private TimelineViewListener listener;

	/**
	 * Constructor that sets all the initial components in the TimelineView.
	 */
	public TimelineView() {

		scroll = new ScrollPane();
		scroll.setPrefSize(DEFAULT_SIZE.getWidth(), DEFAULT_SIZE.getHeight()); // Default
																				// size

		Group root = new Group();

		vbox = new VBox();
		vbox.setSpacing(3);

		dates = new HBox();

		// Create the rows
		for (int i = 0; i < panes.length; i++) {

			panes[i] = new Pane();
			panes[i].setPrefHeight(30);
		}

		vbox.getChildren().addAll(panes);

		stack.getChildren().addAll(dates, vbox);

		root.getChildren().addAll(stack);

		scroll.setContent(root);

		addEventButton = buildButton();
		addEventButton.setTranslateX(-50);
		addEventButton.setTranslateY(-50);
		if (currentTimeline == null) {
			addEventButton.setVisible(false);
		}

		super.getChildren().addAll(scroll, addEventButton);
		super.setAlignment(addEventButton, Pos.BOTTOM_RIGHT);
	}

	/**
	 * Method that sets a timeline to be displayed in the timeline view.
	 * 
	 * @param Timeline
	 */
	public void setTimeline(Timeline timeline) {

		currentTimeline = timeline;
		addEventButton.setVisible(false);
		for (int i = 1; i < panes.length; i++) { // Clear the timeline view.
			panes[i].getChildren().clear();
		}
		drawColumns();

		if (currentTimeline != null) {
			addEventButton.setVisible(true);
			
			// Fetch events from timeline
			ArrayList<Event> events = new ArrayList<Event>();
			if (currentTimeline.getList() != null) {
				for (int i = 0; i < currentTimeline.getList().size(); i++) {
					events.add(currentTimeline.getList().get(i));
				}
			}

			ArrayList<EventShape> shapeList = new ArrayList<EventShape>();

			int trueWidth = width + 1;

			// Convert list of events in timeline to a list of EventShapes.
			for (int i = 0; i < events.size(); i++) {
				EventType type = events.get(i).getType();
				int length;
				int start = (int) (ChronoUnit.DAYS.between(timeline.getStartDate(), events.get(i).getStartDate()));
				if (events.get(i).getType() == EventType.DURATION) {
					length = (int) (ChronoUnit.DAYS.between(events.get(i).getStartDate(), events.get(i).getEndDate()))
							+ 1;
				} else {
					length = 1;
					start++;
				}
				shape = new EventShape(events.get(i), type, start * trueWidth, length * trueWidth);

				shapeList.add(shape);
				
				onMouseOver();
				onMouseExit();
				setOnEventShapeClicked(shape);
			}

			// List that holds all added events.
			ArrayList<EventShape> added = new ArrayList<EventShape>();

			// Algorithm for placing the EventShapes in the timeline view.
			for (int i = 0; i < shapeList.size(); i++) {

				if (i == 0) { // First event.
					panes[1].getChildren().add(shapeList.get(i).getShape());
					added.add(shapeList.get(i));
				} else {
					boolean found = false;
					added.add(shapeList.get(i));

					for (int j = 1; !found; j++) {
						panes[j].getChildren().add(shapeList.get(i).getShape());

						for (int k = 0; k < added.size() - 1; k++) {
							if (shapeList.get(i).isOverlapping(added.get(k))
									&& panes[j].getChildren().contains(added.get(k).getShape())) {
								panes[j].getChildren().remove(shapeList.get(i).getShape());
								found = false;
								break;
							}
							found = true;
						}
					}
				}
			}
		}
	}

	/**
	 * Method that registers the listeners for the timeline view.
	 * 
	 * @param listener
	 */
	public void registerListener(TimelineViewListener listener) {
		this.listener = listener;
		addEventButton.setOnAction(e -> listener.onAddEventClicked((Stage) getScene().getWindow()));
	}

	/**
	 * Private method that draws the columns in the TimelinePane.
	 */
	private void drawColumns() {

		dates.getChildren().clear();

		VBox column;
		Text text;
		Rectangle rect;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int days = 0;

		// Get the duration of the timeline.
		if (currentTimeline != null) {
			LocalDate temp = currentTimeline.getStartDate();
			for (int i = 0; !temp.equals(currentTimeline.getEndDate()); i++) {
				temp = temp.plusDays(1);
				days = i;
			}
			days++;
			
			HBox columns = new HBox();

			// Draw the columns
			for (int i = 0; i <= days; i++) {

				column = new VBox();
				column.setPrefWidth(width);

				rect = new Rectangle();
				rect.setWidth(width);
				rect.setHeight(screenSize.getHeight());
				rect.setStroke(Color.BLACK);
				rect.setOpacity(0.1);
				rect.setFill(Color.WHITE);

				String day = String.valueOf(currentTimeline.getStartDate().plusDays(i).getDayOfMonth());
				text = new Text();
				text.setFont(Font.font("Arial", 18));
				text.setText(day);

				BorderPane txtContainer = new BorderPane();
				txtContainer.setCenter(text);
				txtContainer.setPrefHeight(20);

				column.getChildren().addAll(txtContainer,rect);
				columns.getChildren().add(column);
				
				stack.setPrefSize(Toolkit.getDefaultToolkit().getScreenSize().getWidth(), Toolkit.getDefaultToolkit().getScreenSize().getHeight());

			}
			
			stack.getChildren().add(0, columns);

		}
	}

	/**
	 * Private method that returns an "Add event button".
	 * 
	 * @return Add event button
	 */
	private Button buildButton() {
		final Dimension2D BUTTON_SIZE = new Dimension2D(100, 50);

		Button button = new Button("Add event");
		button.setPrefSize(BUTTON_SIZE.getWidth(), BUTTON_SIZE.getHeight());

		return button;
	}

	private void onMouseOver() {
		EventShape eventshape;

		eventshape = shape;

		eventshape.eventShape.setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				VBox popupVBox = new VBox();
				Text eventID = new Text("Event ID: " + eventshape.event.getId());
				Text eventName = new Text("Event name: " + eventshape.event.getEventName());
				Text eventDescription = new Text("Event description: " + eventshape.event.getDescription());
				Text eventType = new Text("Event type: " + eventshape.event.getType());
				Text eventStart = new Text("Event start date: " + eventshape.event.getStartDate());
				Text eventEnd = new Text("Event end date: " + eventshape.event.getEndDate());
				popupVBox.getChildren().addAll(eventID, eventName, eventDescription, eventType, eventStart, eventEnd);
				hoverOver.setContentNode(popupVBox);
				hoverOver.show(eventshape.eventShape);
			}
		});
	}

	private void onMouseExit() {
		shape.eventShape.setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				hoverOver.hide();
			}
		});
	}
	
	private void setOnEventShapeClicked(EventShape shape) {
		EventShape clicked = shape;
		
		shape.getShape().setOnMouseClicked(e -> {
			listener.onEventClicked((Stage)getScene().getWindow(), clicked.getEvent());
		});
	}
}