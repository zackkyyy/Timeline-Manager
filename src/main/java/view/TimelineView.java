package view;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import interfaces.TimelineViewListener;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
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

	private int rows = 8;
	private int width = 29; // For time perspective
	private Pane[] panes = new Pane[rows];
	private StackPane stack = new StackPane();
	private ScrollPane scroll;
	private VBox vbox;
	private Timeline currentTimeline;
	private Button addEventButton;
	private EventShape shape;
	private PopOver eventWindow = new PopOver();
	private VBox container = new VBox();
	private TimelineViewListener listener;
	private String timePerspective = "Month";
	private Button delete;
	private Button edit;
	private Color textColor = Color.BLACK;
	PopOver test = new PopOver();

	/**
	 * Constructor that sets all the initial components in the TimelineView.
	 */
	public TimelineView() {

		scroll = new ScrollPane();
		scroll.setPrefSize(DEFAULT_SIZE.getWidth(), DEFAULT_SIZE.getHeight());

		Group root = new Group();

		vbox = new VBox();
		vbox.setSpacing(3);

		// Create the rows
		for (int i = 0; i < panes.length; i++) {

			panes[i] = new Pane();
			panes[i].setPrefHeight(30);
		}

		vbox.getChildren().addAll(panes);
		stack.getChildren().add(vbox);
		vbox.setTranslateY(45);
		root.getChildren().addAll(stack);
		scroll.setContent(root);

		addEventButton = AwesomeDude.createIconButton(AwesomeIcon.PLUS_SIGN, "", "30", "30",
				ContentDisplay.GRAPHIC_ONLY);
		addEventButton.setTranslateX(-50);
		addEventButton.setTranslateY(-50);
		if (currentTimeline == null) {
			addEventButton.setVisible(false);
		}

		stack.getChildren().add(0, container);

		super.getChildren().addAll(scroll, addEventButton);
		super.setAlignment(addEventButton, Pos.BOTTOM_RIGHT);
	}

	/**
	 * Method that gets the current timeline.
	 * 
	 * @return Current Timeline
	 */
	public Timeline getTimeline() {
		return currentTimeline;
	}

	/**
	 * Method that sets a timeline to be displayed in the timeline view.
	 * 
	 * @param Timeline
	 */
	public void setTimeline(Timeline timeline, String timePerspective) {

		if (timePerspective.equals("")) {
			timePerspective = this.timePerspective;
		} else {
			this.timePerspective = timePerspective;
		}

		currentTimeline = timeline;
		addEventButton.setVisible(false);

		for (int i = 0; i < panes.length; i++) { // Clear the timeline view.
			panes[i].getChildren().clear();
		}

		drawColumns(timePerspective);

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
				int length;
				int start = (int) (ChronoUnit.DAYS.between(timeline.getStartDate(), events.get(i).getStartDate()));
				if (events.get(i).getType() == EventType.DURATION) {
					length = (int) (ChronoUnit.DAYS.between(events.get(i).getStartDate(), events.get(i).getEndDate()))
							+ 1;
					shape = new EventShape(events.get(i), start * trueWidth, length * trueWidth,
							events.get(i).getColor());
				} else {
					length = 1;
					shape = new EventShape(events.get(i), start * trueWidth + trueWidth / 2, length * trueWidth,
							events.get(i).getColor());
					start++;
				}

				shapeList.add(shape);

				onMouseOver();
				setOnEventShapeClicked(shape);
				setOnEventShapeHover(shape);
			}

			// List that holds all added events.
			ArrayList<EventShape> added = new ArrayList<EventShape>();

			// Algorithm for placing the EventShapes in the timeline view.
			for (int i = 0; i < shapeList.size(); i++) {

				if (i == 0) { // First event.
					panes[0].getChildren().add(shapeList.get(i).getShape());
					added.add(shapeList.get(i));
				} else {
					boolean found = false;
					added.add(shapeList.get(i));

					for (int j = 0; !found; j++) {

						if (j == rows) {
							resize();
						}

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
	 * Returns time perspective;
	 * 
	 * @return time perspective
	 */
	public String getTimePerspective() {
		return timePerspective;
	}

	/**
	 * Sets text color.
	 * 
	 * @param textColor
	 */
	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	/**
	 * Returns text color.
	 * 
	 * @return text color
	 */
	public Color getTextColor() {
		return textColor;
	}

	/**
	 * Method that draws the columns in the TimelineView.
	 */
	private void drawColumns(String timePerspective) {

		if (timePerspective.equals("Week")) {
			width = 90;
		} else if (timePerspective.equals("Month")) {
			width = 29;
		} else if (timePerspective.equals("Year")) {
			width = 5;
		}

		container.getChildren().clear();
		VBox column;
		Text date;
		Rectangle rect;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		stack.setPrefSize(screenSize.getWidth(), screenSize.getHeight());
		int days = 0;
		Pane months = new Pane();

		// Get the duration of the timeline.
		if (currentTimeline != null) {
			LocalDate temp = currentTimeline.getStartDate();
			for (int i = 0; !temp.equals(currentTimeline.getEndDate()); i++) {
				temp = temp.plusDays(1);
				days = i;
			}
			days++;

			HBox columns = new HBox();
			boolean isFirst = true;
			int monthCount = 0;

			// Draw the columns
			for (int i = 0; i <= days || i <= (stack.getPrefWidth() / (width + 1)); i++) {

				String day = String.valueOf(currentTimeline.getStartDate().plusDays(i).getDayOfMonth());
				column = new VBox();

				if (!timePerspective.equals("Year")) {

					column.setPrefWidth(width);
					rect = new Rectangle();
					rect.setWidth(width);
					rect.setHeight(screenSize.getHeight());
					rect.setStroke(Color.BLACK);
					rect.setOpacity(0.1);
					rect.setFill(Color.WHITE);

					date = new Text();
					date.setFont(Font.font("Arial", 18));
					date.setFill(textColor);
					date.setText(day);

					BorderPane txtContainer = new BorderPane();
					txtContainer.setCenter(date);
					txtContainer.setPrefHeight(20);

					String weekDayStr = String.valueOf(currentTimeline.getStartDate().plusDays(i).getDayOfWeek());

					if (i % 2 == 0) {
						weekDayStr = weekDayStr.substring(0, 3);
					} else {
						weekDayStr = "";
					}

					Text weekDay = new Text(weekDayStr);
					weekDay.setFill(textColor);
					txtContainer.setTop(weekDay);
					BorderPane.setAlignment(weekDay, Pos.TOP_CENTER);

					column.getChildren().addAll(txtContainer, rect);
				}

				if (day.equals("1") || Integer.parseInt(day) < 25 && isFirst == true) {
					Text month = new Text(String.valueOf(currentTimeline.getStartDate().plusDays(i).getMonth()) + " "
							+ currentTimeline.getStartDate().plusDays(i).getYear());
					month.setFont(new Font(20));
					month.setFill(textColor);
					months.getChildren().add(month);
					month.setLayoutX(i * (width + 1));
					isFirst = false;

					if (timePerspective.equals("Year")) {
						int yearWidth;
						if (monthCount % 4 == 0)
							yearWidth = (width + 1) * 31;
						else
							yearWidth = (width + 1) * 30;

						if (!day.equals("1")) {
							yearWidth = (31 - Integer.parseInt(day)) * (width + 1);
						}

						Rectangle yearRect = new Rectangle();
						yearRect.setWidth(yearWidth);
						yearRect.setHeight(screenSize.getHeight());
						yearRect.setStroke(Color.BLACK);
						yearRect.setOpacity(0.1);
						yearRect.setFill(Color.WHITE);
						Pane filler = new Pane();
						filler.setMinHeight(38);
						column.getChildren().addAll(filler, yearRect);
					}
					monthCount++;
				}
				columns.getChildren().add(column);
			}
			container.getChildren().addAll(months, columns);
		}
	}

	private void resize() {
		rows += 8;
		Pane[] temp = new Pane[rows];
		for (int i = 0; i < rows; i++) {
			if (i < panes.length)
				temp[i] = panes[i];
			else
				temp[i] = new Pane();
		}

		panes = temp;

		for (int i = 0; i < rows; i++) {
			panes[i] = temp[i];
		}

		vbox.getChildren().clear();
		vbox.getChildren().addAll(panes);
	}

	private void onMouseOver() {
		EventShape eventshape = shape;

		HBox buttonTile = new HBox();

		Pane filler = new Pane();
		filler.setMinWidth(150);

		edit = AwesomeDude.createIconButton(AwesomeIcon.EDIT_SIGN, "", "15", "15", ContentDisplay.CENTER);
		edit.setBackground(Background.EMPTY);
		delete = AwesomeDude.createIconButton(AwesomeIcon.TRASH, "", "15", "15", ContentDisplay.GRAPHIC_ONLY);
		delete.setBackground(Background.EMPTY);
		buttonTile.setSpacing(0);
		buttonTile.getChildren().addAll(filler, delete, edit);
		eventshape.getShape().setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {

				VBox popupVBox = new VBox();
				Text eventName = new Text("Event name: " + eventshape.getEvent().getEventName());
				Text eventDescription = new Text("Event description: " + eventshape.getEvent().getDescription());
				Text eventStart = new Text("Event start date: " + eventshape.getEvent().getStartDate());
				Text eventEnd = new Text("Event end date: " + eventshape.getEvent().getEndDate());

				if (eventshape.getEvent().getType() == EventType.DURATION) {
					popupVBox.getChildren().addAll(buttonTile, eventName, eventDescription, eventStart, eventEnd);
				} else {
					popupVBox.getChildren().addAll(buttonTile, eventName, eventDescription, eventStart);
				}

				eventWindow.setDetached(false);
				eventWindow.setArrowLocation(ArrowLocation.TOP_CENTER);
				eventWindow.setContentNode(popupVBox);
				eventWindow.show(eventshape.getShape());

			}
		});
	}

	private void setOnEventShapeClicked(EventShape shape) {
		EventShape clicked = shape;

		delete.setOnAction(e -> {
			listener.onDeleteEventClicked(clicked.getEvent().getId());
			eventWindow.hide();
		});

		edit.setOnAction(e -> {
			listener.onEditEventClicked((Stage) getScene().getWindow(), clicked.getEvent());
			eventWindow.hide();
		});
	}

	private void setOnEventShapeHover(EventShape shape) {

		EventShape hovered = shape;
		VBox vBox = new VBox();
		Text title = new Text("Event name: " + hovered.getEvent().getEventName());
		vBox.getChildren().add(title);
		test.setContentNode(vBox);

		hovered.getShape().setOnMouseEntered(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				if (hovered.getShape().getLayoutBounds().contains(event.getX(), event.getY())) {
					if (!eventWindow.isShowing()) {
						test.show(hovered.getShape());
					}
				}
			}
		});

		hovered.getShape().setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				test.hide();
			}
		});
	}

}