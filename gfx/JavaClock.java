// ------------------------------------------------------
// JavaClock.java
//
// JavaClock applet
// Version 2.0
// Copyright (c) 1998, 1999 Antony Pranata
// http://www.antonypr.pair.com
//
// Java Clock
// used to display either analog and digital clock
//
// This source code is available freely. You may only use
// it for your personal purpose only (please do not use it
// for commercial purpose).
//
// You may also use this applet on your web page freely
// as long as credit is given (a link to my home page
// is enough).
//
// What's new in this version?
// - This new version of Java Clock support DST
//   I added this feature in this version because many users
//   asked me for it. Actually, I just knew DST when a user
//   ask me: Do your Java Clock support DST? FYI, I live in
//   Indonesia, South East Asia and we do not use DST here
//   (our country lie on equator line).
// ------------------------------------------------------
import java.awt.*;
import java.applet.*;
import java.util.*;

// ------------------------------------------------------
// JavaClock class
// ------------------------------------------------------
public class JavaClock extends Applet implements Runnable
{
  static final Object[][] colors =
    { { "BLACK",     Color.black     },
      { "BLUE",      Color.blue      },
      { "CYAN",      Color.cyan      },
      { "DARKGRAY",  Color.darkGray  },
      { "GRAY",      Color.gray      },
      { "GREEN",     Color.green     },
      { "LIGHTGRAY", Color.lightGray },
      { "MAGENTA",   Color.magenta   },
      { "ORANGE",    Color.orange    },
      { "PINK",      Color.pink      },
      { "RED",       Color.red       },
      { "WHITE",     Color.white     },
      { "YELLOW",    Color.yellow    } };

  // The first field is the name of a city
  // The second field is a difference time from GMT
  // The third field is 1 if the city support DST, 0 otherwise
  static final Object[][] cityZone =
    { { "Hawaii",        "-600","0" },
      { "Alaska",        "-540", "1" },
      { "Pacific Time",  "-480", "1" },
      { "Arizona",       "-420", "0" },
      { "Mountain Time", "-420", "1" },
      { "Central Time",  "-360", "1" },
      { "Mexico",        "-360", "1" },
      { "Eastern Time",  "-300", "1" },
      { "Atlantic Time", "-240", "1" },
      { "Brasilia",      "-180", "1" },
      { "GMT",              "0", "1" },
      { "Paris",           "60", "1" },
      { "Cairo",          "120", "1" },
      { "Israel",         "120", "1" },
      { "Moscow",         "180", "1" },
      { "New Delhi",      "330", "0" },
      { "Jakarta",        "420", "0" },
      { "Hong Kong",      "480", "0" },
      { "Beijing",        "480", "0" },
      { "Tokyo",          "540", "0" },
      { "Adelaide",       "570", "1" },
      { "Darwin",         "570", "0" },
      { "Sydney",         "600", "1" },
      { "Auckland",       "720", "1" } };

  Thread threadClock; // thread of the clock

  boolean isAnalog = true; // true if analog clock is displayed
  Font fontText = null; // font used to display digital clock
  Color fontColor = Color.black; // font color;
  Color backColor = Color.lightGray; // background color
  Color hHandColor = Color.blue; // hour hand color
  Color mHandColor = Color.blue; // minute hand color
  Color sHandColor = Color.black; // second hand color
  Color hPointColor = Color.red; // hour point color
  Color mPointColor = Color.lightGray; // hour point color

  int xPoint[] = new int[4];
  int yPoint[] = new int[4];

  Image backImage; // background image
  MediaTracker tracker;
  Image imageBuffer; // buffer image

  int fromGMT; // local time - GMT time
  boolean isDST; // true if the city/country use DST
  boolean isLocalUseDST; // true if local time use DST
  int currentZone; // current time zone
  int oldHour = -1, oldMinute = -1, oldSecond = -1;

  // ----------------------------------------------------
  // setZone method, set time zone
  // ----------------------------------------------------
  public void setCityZone (String zone) {
    currentZone = fromGMT;
    isDST = isLocalUseDST;
    for (int i = 0; i < cityZone.length; i++)
      if (zone.indexOf ((String)cityZone[i][0]) != -1)
      {
        currentZone = Integer.parseInt ((String)cityZone[i][1]);
        isDST = (Integer.parseInt ((String)cityZone[i][2]) == 1);
      }
  }

  // ----------------------------------------------------
  // setIsLocalUseDST method
  // ----------------------------------------------------
  public void setIsLocalUseDST (boolean usedst) {
    isLocalUseDST = usedst;
  }

  // ----------------------------------------------------
  // findColor method, parse a string into color
  // ----------------------------------------------------
  private Color findColor (String param, Color defColor) {
    if (param != null) {
      param = param.toUpperCase ();
      if (param.charAt (0) == '#') {
        return new Color (Integer.parseInt (param.substring (1), 16));
      }
      else {
        for (int i = 0; i < colors.length; i++)
          if (param.compareTo ((String)colors[i][0]) == 0) {
            return (Color)colors[i][1];
          }
      }
    }
    return defColor;
  }

  // ----------------------------------------------------
  // init method, applet initialization
  // ----------------------------------------------------
  public void init () {
    // Read "typeface" & "fontsize" parameter from HTML file
    String param = getParameter ("typeface");
    if (param == null)
      param = "Helvetica";

    int paramSize;
    try {
      paramSize = Integer.parseInt (
        getParameter ("fontsize"), 10);
    }
    catch (NumberFormatException e) {
      paramSize = 16;
    }

    fontText = new Font (param, Font.PLAIN, paramSize);

    // Read color parameter from HTML file
    backColor = findColor (getParameter ("backcolor"), backColor);
    fontColor = findColor (getParameter ("fontcolor"), fontColor);
    hHandColor = findColor (getParameter ("hhandcolor"), hHandColor);
    mHandColor = findColor (getParameter ("mhandcolor"), mHandColor);
    sHandColor = findColor (getParameter ("shandcolor"), sHandColor);
    hPointColor = findColor (getParameter ("hpointcolor"), hPointColor);
    mPointColor = findColor (getParameter ("mpointcolor"), mPointColor);

    // Read "backimage" parameter from HTML file
    tracker    = new MediaTracker (this);
    param = getParameter ("backimage");
    if (param != null) {
      backImage = getImage (getCodeBase (), param);
      tracker.addImage (backImage, 0);
    }
    else
      backImage = null;

    // "analog" parameter
    param = getParameter ("analog");
    if ((param != null) && (param.indexOf ("false") > -1))
      isAnalog = false;
    else
      isAnalog = true;

    // "dst" parameter
    param = getParameter ("dst");
    if ((param != null) && (param.indexOf ("false") > -1))
      isLocalUseDST = false;
    else
      isLocalUseDST = true;
    isDST = isLocalUseDST;

    // Find the time zone
    Date localDate = new Date ();
    fromGMT = -localDate.getTimezoneOffset ();
    currentZone = fromGMT;
  }

  // ----------------------------------------------------
  // start method
  // ----------------------------------------------------
  public void start () {
    // Create the buffer
    if (imageBuffer == null) {
      Dimension dim = size ();
      imageBuffer = createImage (dim.width, dim.height);
    }

    // Create the thread
    if (threadClock == null) {
      threadClock = new Thread (this);
      threadClock.start ();
    }
  }

  // ----------------------------------------------------
  // stop method
  // ----------------------------------------------------
  public void stop () {
    // Stop the thread
    if (threadClock != null) {
      threadClock.stop ();
      threadClock = null;
      imageBuffer = null;
    }
  }

  // ----------------------------------------------------
  // run method
  // ----------------------------------------------------
  public void run () {
    // Wait until background image loaded
    try {
      tracker.waitForAll ();
    }
    catch (InterruptedException e) {
      return;
    }

    // If current thread is threadClock then ...
    while (Thread.currentThread () == threadClock) {
      // Draw the clock
      repaint ();

      // Delay for 500 ms
      try {
        threadClock.sleep (500);
      }
      catch (InterruptedException e) {
        break;
      }
    }
  }

  // ----------------------------------------------------
  // IsFirstAprLastOct method
  // ----------------------------------------------------
  boolean IsFirstAprLastOct (Date checkDate) {
    int dd = checkDate.getDate ();
    int mm = checkDate.getMonth ();
    if ((mm >= 3) && (mm <= 9)) {
      Date dateApril = new Date (checkDate.getYear (), 3, 1);
      int firstWeekApril = 8 - dateApril.getDay ();
      if (firstWeekApril > 7)
        firstWeekApril -= 7;

      Date dateOct = new Date (checkDate.getYear (), 9, 31);
      int lastWeekOct = 31 - dateOct.getDay ();

      boolean isSetAhead1 =
        ((mm == 3) && (dd > firstWeekApril)) ||
        ((mm == 3) && (dd == firstWeekApril) && (checkDate.getHours () >= 2)) ||
        (mm > 3);
      boolean isSetAhead2 =
        ((mm == 9) && (dd < lastWeekOct)) ||
        ((mm == 9) && (dd == lastWeekOct) && (checkDate.getHours () < 2)) ||
        (mm < 9);
      if (isSetAhead1 && isSetAhead2)
        return true;
      else
        return false;
    }
    return false;
  }

  // ----------------------------------------------------
  // update method
  // ----------------------------------------------------
  public void update (Graphics g) {
    Date newTime = new Date ();
    long timeDest = newTime.getTime ();
    timeDest += (currentZone - fromGMT) * 60 * 1000;
    newTime = new Date (timeDest);

    int hour   = newTime.getHours ();

    // Set DST
    if ((isDST) && (!isLocalUseDST)) {
      if (IsFirstAprLastOct (newTime))
        hour += 1;
    }

    if ((!isDST) && (isLocalUseDST)) {
      Date newTime2 = new Date ();
      if (IsFirstAprLastOct (newTime2))
        hour -= 1;
    }

    int minute = newTime.getMinutes ();
    int second = newTime.getSeconds ();

    if ((hour != oldHour) || (minute != oldMinute) ||
        (second != oldSecond)) {
      // Draw background in the buffer
      Graphics graphBuffer = imageBuffer.getGraphics ();
      DrawBackground (graphBuffer);

      Dimension dim = size ();
      int centerX = dim.width >> 1;
      int centerY = dim.height >> 1;

      if (isAnalog) {
        // Prepare the drawing of the analog clock
        double radius1 = Math.min (centerX, centerY) * 0.75;
        double radius2 = Math.min (centerX, centerY) * 0.04;

        // Draw minute hand
        double posMinute = Math.PI * (minute / 30.0 + second / 1800.0);
        xPoint[0] = (int)Math.round (centerX - 2 * radius2 * Math.sin (posMinute)) - 1;
        xPoint[1] = (int)Math.round (centerX - radius2 * Math.cos (posMinute));
        xPoint[2] = (int)Math.round (centerX + radius1 * Math.sin (posMinute)) + 1;
        xPoint[3] = (int)Math.round (centerX + radius2 * Math.cos (posMinute));

        yPoint[0] = (int)Math.round (centerY + 2 * radius2 * Math.cos (posMinute)) - 1;
        yPoint[1] = (int)Math.round (centerY - radius2 * Math.sin (posMinute));
        yPoint[2] = (int)Math.round (centerY - radius1 * Math.cos (posMinute)) + 1;
        yPoint[3] = (int)Math.round (centerY + radius2 * Math.sin (posMinute));

        graphBuffer.setColor (mHandColor);
        graphBuffer.fillPolygon (xPoint, yPoint, 4);

        if (minute < 30)
          graphBuffer.setColor (Color.white);
        else
          graphBuffer.setColor (Color.black);
        graphBuffer.drawLine (xPoint[0], yPoint[0], xPoint[1], yPoint[1]);
        graphBuffer.drawLine (xPoint[1], yPoint[1], xPoint[2], yPoint[2]);

        if (minute < 30)
          graphBuffer.setColor (Color.black);
        else
          graphBuffer.setColor (Color.white);
        graphBuffer.drawLine (xPoint[2], yPoint[2], xPoint[3], yPoint[3]);
        graphBuffer.drawLine (xPoint[3], yPoint[3], xPoint[0], yPoint[0]);

        // Draw hour hand
        double posHour = Math.PI * (hour / 6.0 + minute / 360.0);
        radius1 = Math.min (centerX, centerY) * 0.5;
        radius2 = Math.min (centerX, centerY) * 0.05;
        xPoint[0] = (int)Math.round (centerX - 2 * radius2 * Math.sin (posHour)) - 1;
        xPoint[1] = (int)Math.round (centerX - radius2 * Math.cos (posHour));
        xPoint[2] = (int)Math.round (centerX + radius1 * Math.sin (posHour)) + 1;
        xPoint[3] = (int)Math.round (centerX + radius2 * Math.cos (posHour));

        yPoint[0] = (int)Math.round (centerY + 2 * radius2 * Math.cos (posHour)) - 1;
        yPoint[1] = (int)Math.round (centerY - radius2 * Math.sin (posHour));
        yPoint[2] = (int)Math.round (centerY - radius1 * Math.cos (posHour)) + 1;
        yPoint[3] = (int)Math.round (centerY + radius2 * Math.sin (posHour));

        graphBuffer.setColor (hHandColor);
        graphBuffer.fillPolygon (xPoint, yPoint, 4);

        if (((hour >= 0) && (hour <= 6)) || ((hour >= 12) && (hour <= 18)))
          graphBuffer.setColor (Color.white);
        else
          graphBuffer.setColor (Color.black);
        graphBuffer.drawLine (xPoint[0], yPoint[0], xPoint[1], yPoint[1]);
        graphBuffer.drawLine (xPoint[1], yPoint[1], xPoint[2], yPoint[2]);

        if (((hour >= 0) && (hour <= 6)) || ((hour >= 12) && (hour <= 18)))
          graphBuffer.setColor (Color.black);
        else
          graphBuffer.setColor (Color.white);
        graphBuffer.drawLine (xPoint[2], yPoint[2], xPoint[3], yPoint[3]);
        graphBuffer.drawLine (xPoint[3], yPoint[3], xPoint[0], yPoint[0]);

        // Draw second hand
        radius1 = Math.min (centerX, centerY) * 0.75;
        graphBuffer.setColor (sHandColor);
        double posSecond = Math.PI * second / 30.0;
        graphBuffer.drawLine (
          centerX, centerY,
          (int)Math.round (centerX + radius1 * Math.sin (posSecond)),
          (int)Math.round (centerY - radius1 * Math.cos (posSecond)));
      }
      else {
        // Draw digital clock
        graphBuffer.setFont (fontText);
        graphBuffer.setColor (fontColor);

        String displayTime =
          ((hour < 10) ? ("0" + hour) : Integer.toString (hour)) + ":" +
          ((minute < 10) ? ("0" + minute) : Integer.toString (minute)) + ":" +
          ((second < 10) ? ("0" + second) : Integer.toString (second));

        FontMetrics fm = graphBuffer.getFontMetrics ();
        int LokY = dim.height >> 1;
        if (LokY < 0)
          LokY = 0;

        int LokX = (dim.width - fm.stringWidth (displayTime)) >> 1;
        if (LokX < 0)
          LokX = 0;

        graphBuffer.drawString (displayTime, LokX, LokY);
      }

      oldHour = hour;
      oldMinute = minute;
      oldSecond = second;
    }

    // Copy the buffer to the "screen"
    g.drawImage (imageBuffer, 0, 0, null);
  }

  // ----------------------------------------------------
  // paint method
  // ----------------------------------------------------
  public void paint (Graphics g) {
    // If there's error, draw nothing to the screen
    Dimension dim = size ();
    if (tracker.isErrorAny ()) {
      g.setColor (Color.white);
      g.fillRect (0, 0, dim.width, dim.height);
      return;
    }

    // If there's NO error, draw the image to the screen
    if (tracker.checkAll (true))
      DrawBackground (g);
  }

    // ----------------------------------------------------
  // mouseDown method
  // ----------------------------------------------------
  public boolean mouseDown (Event evt, int x, int y) {
    isAnalog = !isAnalog;
    return true;
  }

  // ----------------------------------------------------
  // DrawBackground method
  // ----------------------------------------------------
  private void DrawBackground (Graphics g) {
    // Fill background with solid color
    Dimension dim = size ();
    g.setColor (backColor);
    g.fillRect (0, 0, dim.width, dim.height);

    // Draw background image
    if (backImage != null) {
      int w = backImage.getWidth (this);
      int h = backImage.getHeight (this);
      if ((w < 0) || (h < 0))
        return;

      g.drawImage (backImage, (
        dim.width - w) >> 1, (dim.height - h) >> 1, null);
    }

    if (isAnalog) {
      // Draw hour
      int centerX = dim.width >> 1;
      int centerY = dim.height >> 1;
      double radius = Math.min (centerX, centerY) * 0.9;
      for (int i = 1; i <= 12; i++) {
        double buffer = Math.PI * (0.5 - i / 6.0);
        int posX = (int)Math.floor (centerX + radius * Math.cos (buffer));
        int posY = (int)Math.floor (centerY - radius * Math.sin (buffer));

        g.setColor (hPointColor);
        g.fill3DRect (posX - 2, posY - 2, 4, 4, true);
      }

      // Draw minute
      for (int i = 1; i <= 60; i++) {
        if ((i % 5) != 0) {
          double buffer = Math.PI * i / 30.0;
          int posX = (int)Math.floor (centerX + radius * Math.cos (buffer));
          int posY = (int)Math.floor (centerY - radius * Math.sin (buffer));

          g.setColor (mPointColor);
          g.fill3DRect (posX - 2, posY - 2, 3, 3, false);
        }
      }
    }
  }
}