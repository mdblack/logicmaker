package org.blackware.logicmaker;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Stack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Datapath extends View implements View.OnTouchListener
{
	private Logicmaker logicmaker;
	
	private DatapathModule defaultModule;
	public double scaling=4.0;
	private int xshift=0,yshift=0;
	private int dpwidth=2000, dpheight=2000;
	private int gridsize=2;
	private int decodersize=3;
	private int defaultbits=1;	
	private Block tempblock=null;
	public Bus tempbus1=null;

	private Bus tempbus2=null;
	private CustomProcessorModule processorModule;

	private class ErrorEntry{int number; String error; public ErrorEntry(int number, String error){this.number=number; this.error=error;}}
	public ArrayList<ErrorEntry> errorlog;	

	public String action="";
	public String component="";
	
	private int corner1x,corner1y,corner2x,corner2y,mousex,mousey;
	private Stack<String> undolog;
	public boolean isSimulating;
	private GestureDetector gestureDetector;
	private ScaleGestureDetector scaleDetector;
	
	public Datapath(Context context) 
	{
		super(context);
		logicmaker=(Logicmaker)context;
		
		gestureDetector=new GestureDetector(new GestureListener());
		scaleDetector=new ScaleGestureDetector(logicmaker,new GestureListener());
		
		defaultModule=new DatapathModule();
		resetAll();

		undolog=new Stack<String>();
		errorlog=new ArrayList<ErrorEntry>();
		undolog.push(dumpXML());
	
	}
	
	public void setStatusLabel(final String s)
	{
/*		logicmaker.handler.post(new Runnable(){
			public void run() {
				Toast.makeText(logicmaker, s, Toast.LENGTH_SHORT).show();
				
			}});*/
		logicmaker.statustext.setText(s);
	}
	
	public void clearAction()
	{
		action="";
		component="";
		tempblock=null;
		tempbus1=null;
		tempbus2=null;
		setStatusLabel("");
		logicmaker.placespinner.setSelection(0);
		for(Block b:defaultModule.blocks)
		{
			b.highlighted=false;
		}
		for(Bus b:defaultModule.buses)
		{
			b.highlighted=false;
		}
		postInvalidate();
	}
	private void clearBusAction()
	{
		tempblock=null;
		tempbus1=null;
		tempbus2=null;
		for(Block b:defaultModule.blocks)
		{
			b.highlighted=false;
		}
		for(Bus b:defaultModule.buses)
		{
			b.highlighted=false;
		}
		postInvalidate();
		setStatusLabel("Press the mouse on a block and drag to draw a bus");
	}
	public void clearAll()
	{
		defaultModule.blocks=new ArrayList<Block>();
		defaultModule.buses=new ArrayList<Bus>();
		defaultModule.blocknumber=1;
		postInvalidate();
	}
	public void unselectAll()
	{
		for (Block b:defaultModule.blocks)
			b.unselect();
		for (Bus b:defaultModule.buses)
			b.unselect();
		errorlog=new ArrayList<ErrorEntry>();
		postInvalidate();
	}
	public boolean verify()
	{
		errorlog=new ArrayList<ErrorEntry>();
		for (Block b:defaultModule.blocks)
		{
			if (!b.verify())
				b.selected=true;
			else
				b.selected=false;
		}
		for (Bus b:defaultModule.buses)
		{
			if (!b.verify())
				b.selected=true;
			else
				b.selected=false;
		}
		if (errorlog.size()>0)
			setStatusLabel("There are errors in the datapath.  Bad blocks are highlighted.");
		else
			setStatusLabel("Success: no errors were found in the datapath");
		postInvalidate();
		return errorlog.size()==0;
	}
	
	public void dosave(String filename)
	{
		try
		{
			PrintWriter p=new PrintWriter(filename);
			String s=dumpXML();
			p.print(s);
			p.close();
			System.out.println(s);
			setStatusLabel("Saved to "+filename);
		}
		catch(IOException e)
		{
			setStatusLabel("Unable to save to "+filename);
		}
	}
	public void doloadxml(String xml)
	{
		undolog.push(dumpXML());
		DatapathXMLParse xmlParse=new DatapathXMLParse(xml,defaultModule);
		for (int i=1; i<=xmlParse.highestBlockNumber(); i++)
			xmlParse.constructBlock(i);
		
		postInvalidate();		
	}
	public void doload(String filename)
	{
		try
		{
			undolog.push(dumpXML());
			String xml="";

			FileReader r=new FileReader(filename);
				
				Scanner s=new Scanner(r);
				while(s.hasNextLine())
					xml+=s.nextLine()+" ";
				s.close();

			defaultModule.basenumber=defaultModule.blocknumber;
			
			DatapathXMLParse xmlParse=new DatapathXMLParse(xml,defaultModule);
			for (int i=1; i<=xmlParse.highestBlockNumber(); i++)
				xmlParse.constructBlock(i);
			
			postInvalidate();
			
			setStatusLabel("Loaded "+filename);
		}
		catch(IOException e)
		{
			setStatusLabel("Unable to load "+filename);
		}		
	}
	
	public void simulate()
	{
		if (!verify())
		{
			setStatusLabel("Can't simulate: there are errors in the datapath");
			return;
		}
		isSimulating=true;
		unselectAll();
		clearAction();
		
		processorModule=new CustomProcessorModule(defaultModule);
		processorModule.active=true;
		processorModule.updateGUIs=true;
	}	
	//remove a component
	public void delete()
	{
		//save undo info
		undolog.push(dumpXML());
		ArrayList<Block> removelist=new ArrayList<Block>();
		ArrayList<Bus> removebuslist=new ArrayList<Bus>();
		//make a remove list first so nothing is disrupted when removing
		for (Bus b: defaultModule.buses)
		{
			if (b.selected)
				removebuslist.add(b);		
		}
		for (Bus b: removebuslist)
		{			
			//if the bus is sourced by a splitter, remove bus from splitter list
			if (b.input!=0 && defaultModule.getBlock(b.input)!=null && defaultModule.getBlock(b.input).type.equals("splitter"))
			{
				Block splitter=(Block)b.getInputBlocks()[0];
				for (Enumeration e=splitter.bus.keys(); e.hasMoreElements();)
				{
					Integer splitterkey=(Integer)(e.nextElement());
					int i=splitterkey.intValue();

					if (b.number==i)
						splitter.bus.remove(splitterkey);
				}				
			}
			//now remove the bus
			defaultModule.buses.remove(b);
			//and unlink any buses sourcing or sinking this bus
			for (Bus bb: defaultModule.buses)
			{
				if (bb.input==b.number)
					bb.input=0;
				if (bb.output==b.number)
					bb.output=0;
			}
		}
		//make a remove list of blocks
		for (Block b:defaultModule.blocks)
		{
			if (b.selected)
				removelist.add(b);
		}
		//remove the blocks, and unlink any buses connected to them
		for (Block b: removelist)
		{
			defaultModule.blocks.remove(b);
			for (Bus bb: defaultModule.buses)
			{
				if (bb.input==b.number)
					bb.input=0;
				if (bb.output==b.number)
					bb.output=0;
			}
		}
		postInvalidate();
	}
/*	public void clockAll()
	{
		defaultModule.clockAll();
		postInvalidate();
	}*/
	public void resetClocks()
	{
		defaultModule.resetClocks();
	}	
	public void resetHighlights()
	{
		defaultModule.resetHighlights();
	}	
	public void resetAll()
	{
		defaultModule.resetAll();
		postInvalidate();
	}
	public void propagateAll()
	{
		defaultModule.propagateAll();
	}
	public String dumpXML()
	{
		return defaultModule.dumpXML();
	}
	public String[] controlOutputs()
	{
		return defaultModule.controlOutputs();
	}

	public String[] controlInputs()
	{
		return defaultModule.controlInputs();
	}
	
	protected void onDraw(Canvas g)
	{
		Paint paint=new Paint();
		paint.setColor(Color.WHITE);
		g.drawRect(0,0,dpwidth,dpheight,paint);
		for (Block b:defaultModule.blocks)
			b.draw(g);
		for (Bus b:defaultModule.buses)
			b.draw(g);
		if (action.equals("place") && tempblock!=null)
			tempblock.draw(g);
		if (action.equals("place") && tempbus1!=null)
			tempbus1.draw(g);
		if (action.equals("place") && tempbus2!=null)
			tempbus2.draw(g);
	}


	public boolean onTouch(View v, MotionEvent event) 
	{
		scaleDetector.onTouchEvent(event);
		gestureDetector.onTouchEvent(event);
		
		switch (event.getActionMasked())
		{
		case MotionEvent.ACTION_POINTER_DOWN:
			break;
		case MotionEvent.ACTION_DOWN:
			tryStartBus(event);
			tryActionDown(event);
			break;
		case MotionEvent.ACTION_UP:
			tryEndBus(event);
			tryActionUp(event);
			break;
		case MotionEvent.ACTION_MOVE:
			tryDrawBus(event);
			break;
		}
		return true;
	}
	
	private class GestureListener implements OnGestureListener, OnScaleGestureListener
	{
		public boolean onDown(MotionEvent e) {
			return false;
		}
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return false;
		}
		public void onLongPress(MotionEvent e) {
			tryEditBlock(e);
		}
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			boolean didmove=false;
			if (action.equals(""))
			{
				if (!isSimulating)
				{
					
					for (Block b:defaultModule.blocks)
					{
						if (b.selected)
						{
							didmove=true;
						}
					}
					for (Bus b:defaultModule.buses)
					{
						if (b.selected)
						{
							didmove=true;
						}
					}
					if (didmove)
					{
						distanceX=(float)(distanceX/scaling);
						distanceY=(float)(distanceY/scaling);
						undolog.push(dumpXML());
						for (Block b:defaultModule.blocks)
						{
							if (b.selected)
							{
								b.xcoor-=distanceX;
								b.ycoor-=distanceY;
								b.xcoor2-=distanceX;
								b.ycoor2-=distanceY;
							}
						}
						for (Bus b:defaultModule.buses)
						{
							if (b.selected)
							{
								b.xcoor-=distanceX;
								b.ycoor-=distanceY;
								b.xcoor2-=distanceX;
								b.ycoor2-=distanceY;
							}
						}
						postInvalidate();						
					}
				}
				if(!didmove)
				{
					xshift-=distanceX;
					yshift-=distanceY;
					postInvalidate();
				}
				return true;
			}
			return false;
		}
		public void onShowPress(MotionEvent e) {
		}
		public boolean onSingleTapUp(MotionEvent e) {
			tryPlaceBlock(e);
			tryActionClick(e);
			if(!trySelectBlock(e))
				tryClockAll();
			return true;
		}
		public boolean onScale(ScaleGestureDetector detector) {
			scaling*=detector.getScaleFactor();
			if (scaling<1.0) scaling=1.0;
			postInvalidate();
			return true;
		}
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}
		public void onScaleEnd(ScaleGestureDetector detector) {
		}
		
	}
	
	private void tryActionDown(MotionEvent arg0)
	{
		if (action.equals("massselect"))
		{
			setStatusLabel("Drag and release to select blocks");
			corner1x=getX(arg0);
			corner1y=getY(arg0);
		}
	}
	private void tryActionClick(MotionEvent arg0)
	{
		if (action.equals("duplicate"))
		{
			setStatusLabel("Click on the top left of where you want to paste");
			duplicate(getX(arg0),getY(arg0));					
		}		
	}
	private void tryActionUp(MotionEvent arg0)
	{
		if (action.equals("massselect"))
		{
			//make sure corner1 is top left and corner2 bottom right
			if (corner1x<getX(arg0))
			{
				corner2x=getX(arg0);
			}
			else
			{
				corner2x=corner1x;
				corner1x=getX(arg0);
			}
			if (corner1y<getY(arg0))
			{
				corner2y=getY(arg0);
			}
			else
			{
				corner2y=corner1y;
				corner1y=getY(arg0);
			}
			//go through all the pixels between corners and select blocks
			for(int x=corner1x; x<=corner2x; x++)
			{
				for (int y=corner1y; y<=corner2y; y++)
				{
					for (Block b:defaultModule.blocks)
					{
						if (b.doSelect(x,y) && !b.selected)
							b.select();
					}
					for (Bus b:defaultModule.buses)
					{
						if (b.doSelect(x,y) && !b.selected)
							b.select();
					}							
				}
			}
			clearAction();
		}
	}
	
	private void tryClockAll()
	{
		if (isSimulating)
		{
			processorModule.doCycle();
			postInvalidate();
		}
		
	}
	
	private boolean tryEditBlock(MotionEvent arg0)
	{
		if (!action.equals(""))
		{
			if (action.equals("place")&&component.equals("bus")&&tempbus1!=null)
				return false;
			clearAction();
			return false;
		}
		for (Block b:defaultModule.blocks)
		{
			if (b.doSelect(getX(arg0), getY(arg0)))
			{
				editBlock(b);
				return true;
			}
		}
		for (Bus b:defaultModule.buses)
		{
			if (b.doSelect(getX(arg0), getY(arg0)))
			{
				editBlock(b);
				return true;
			}
		}
		clearAction();
		return false;
	}
	
	private boolean tryPlaceBlock(MotionEvent arg0)
	{
		if (action.equals("place"))
		{
			//if not a bus, create the block at the coordinates and add it to the list
			if (!component.equals("bus"))
			{
				Block newb=new Block(component,defaultbits,defaultModule);
				newb.place(getX(arg0),getY(arg0));
				defaultModule.addBlock(newb);
				postInvalidate();
				return true;
			}
		}
		return false;
	}
	
	private boolean tryStartBus(MotionEvent arg0)
	{
		boolean retval=false;
		if (action.equals("place") && component.equals("bus"))
		{
			//find out who is sourcing it
			Block b=null;
			Bus bu=null;
			for (Block bl:defaultModule.blocks)
				if (bl.getXExit(getX(arg0),getY(arg0))!=-1 && bl.getYExit(getX(arg0),getY(arg0))!=-1)
				{
					b=bl;
					break;
				}
			for (Bus bl: defaultModule.buses)
				if (bl.getXExit(getX(arg0),getY(arg0))!=-1 && bl.getYExit(getX(arg0),getY(arg0))!=-1)
				{
					bu=bl;
					break;
				}
			//a bus must have a source
			if (b==null && bu==null)
				return false;

			setStatusLabel("Release at bus endpoint");
			//if it's sourced by a block, it will be vertical and start from the block's exit
			if (b!=null)
			{
				tempbus1=new Bus(b,defaultModule);
				tempbus1.isHorizontal=false;
				tempbus1.xcoor=b.getXExit(getX(arg0), getY(arg0));
				tempbus1.ycoor=b.getYExit(getX(arg0), getY(arg0));
				tempbus1.bits=b.getBits(tempbus1.xcoor);
				b.highlighted=true;
			}
			//if it's sourced by a bus, it will be perpendicular to the bus
			else
			{
				tempbus1=new Bus(bu,defaultModule);
				tempbus1.isHorizontal=!bu.isHorizontal;
				tempbus1.xcoor=bu.getXExit(getX(arg0), getY(arg0));
				tempbus1.ycoor=bu.getYExit(getX(arg0), getY(arg0));
				bu.highlighted=true;
			}
			//set up a second bus to turn corners
			tempbus1.xcoor2=tempbus1.xcoor;
			tempbus1.ycoor2=tempbus1.ycoor;
			tempbus2=new Bus(tempbus1,defaultModule);
			tempbus2.isHorizontal=!tempbus1.isHorizontal;
			if (b!=null)
			{
				tempbus2.xcoor=b.getXExit(getX(arg0), getY(arg0));
				tempbus2.ycoor=b.getYExit(getX(arg0), getY(arg0));
			}
			else
			{
				tempbus2.xcoor=bu.getXExit(getX(arg0), getY(arg0));
				tempbus2.ycoor=bu.getYExit(getX(arg0), getY(arg0));							
			}
			tempbus2.xcoor2=tempbus2.xcoor;
			tempbus2.ycoor2=tempbus2.ycoor;
			retval=true;
			postInvalidate();
		}
		return retval;
	}
	
	private boolean tryDrawBus(MotionEvent arg0)
	{
		//if dragging and a bus is started, show where the bus is going
		//if the bus looks like it will connect a component, highlight that component
		if (action.equals("place")&&component.equals("bus")&&tempbus1!=null)
		{
			if (tempbus1.isHorizontal)
			{
				tempbus1.xcoor2=getX(arg0);
				tempbus2.xcoor=getX(arg0);
				tempbus2.xcoor2=getX(arg0);
				tempbus2.ycoor2=getY(arg0);
			}
			else
			{
				tempbus1.ycoor2=getY(arg0);
				tempbus2.ycoor=getY(arg0);
				tempbus2.ycoor2=getY(arg0);
				tempbus2.xcoor2=getX(arg0);						
			}
			for(Block b:defaultModule.blocks)
			{
				if (b.getXEntrance(getX(arg0),getY(arg0))!=-1 && b.getYEntrance(getX(arg0),getY(arg0))!=-1)
					b.highlighted=true;
				else
					b.highlighted=false;
			}
			for(Bus bu:defaultModule.buses)
			{
				if (bu.input!=0) continue;
				if (bu.getXEntrance(getX(arg0),getY(arg0))!=-1 && bu.getYEntrance(getX(arg0),getY(arg0))!=-1)
					bu.highlighted=true;
				else
					bu.highlighted=false;
			}
			postInvalidate();
			return true;
		}
		return false;		
	}
	
	private boolean tryEndBus(MotionEvent arg0)
	{
		if (action.equals("place")&&component.equals("bus")&&tempbus1!=null)
		{
			//get all the coordinates
			if (tempbus1.isHorizontal)
			{
				tempbus1.xcoor2=getX(arg0);
				tempbus2.xcoor=getX(arg0);
				tempbus2.xcoor2=getX(arg0);
				tempbus2.ycoor2=getY(arg0);
			}
			else
			{
				tempbus1.ycoor2=getY(arg0);
				tempbus2.ycoor=getY(arg0);
				tempbus2.ycoor2=getY(arg0);
				tempbus2.xcoor2=getX(arg0);						
			}
			//check if either bus is invalid and diagonal
			if (tempbus1.xcoor!=tempbus1.xcoor2 && tempbus1.ycoor!=tempbus1.ycoor2)
			{
				clearBusAction();
				postInvalidate();
				return false;						
			}
			if (tempbus2.xcoor!=tempbus1.xcoor2 && tempbus1.ycoor!=tempbus2.ycoor2)
			{
				clearBusAction();
				postInvalidate();
				return false;						
			}
			//instantiate bus 1
			Bus b1=tempbus1;
			b1.place(tempbus1.xcoor, tempbus1.ycoor, tempbus1.xcoor2, tempbus1.ycoor2, tempbus1.input, 0);
			//if bus one is a single pixel, replace it with bus 2
			if (b1.xcoor==b1.xcoor2 && b1.ycoor==b1.ycoor2)
			{
				b1=tempbus2;
				b1.place(tempbus2.xcoor, tempbus2.ycoor, tempbus2.xcoor2, tempbus2.ycoor2, b1.number, 0);
				//if both buses are a single pixel, cancel the operation
				if (b1.xcoor==b1.xcoor2 && b1.ycoor==b1.ycoor2)
				{
					clearBusAction();
					postInvalidate();
					return false;
				}
			}

			//find out which block this new bus is sourcing
			for (Block bl:defaultModule.blocks)
			{
				if (bl.getXEntrance(b1.xcoor2,b1.ycoor2)!=-1 && bl.getYEntrance(b1.xcoor2,b1.ycoor2)!=-1)
				{
					b1.output=bl.number;
					b1.xcoor2=bl.getXEntrance(b1.xcoor2,b1.ycoor2);
					b1.ycoor2=bl.getYEntrance(b1.xcoor2,b1.ycoor2);
					clearBusAction();
					if (b1.xcoor!=b1.xcoor2 && b1.ycoor!=b1.ycoor2)
						{
						postInvalidate();
						return false;
						}
					defaultModule.addBlock(b1);
					postInvalidate();
					return true;
				}
			}
			//no block? then look for a bus
			for (Bus bu:defaultModule.buses)
			{
				//only source if it's an orphan bus
				if (bu.input!=0) continue;
				
				if (bu.getXEntrance(b1.xcoor2,b1.ycoor2)!=-1 && bu.getYEntrance(b1.xcoor2,b1.ycoor2)!=-1)
				{
					b1.output=bu.number;
					b1.xcoor2=bu.getXEntrance(b1.xcoor2,b1.ycoor2);
					b1.ycoor2=bu.getYEntrance(b1.xcoor2,b1.ycoor2);
					clearBusAction();
					if (b1.xcoor!=b1.xcoor2 && b1.ycoor!=b1.ycoor2)
						{
						postInvalidate();
						return false;
						}
					defaultModule.addBlock(b1);
					bu.input=b1.number;
					b1.output=bu.number;
					postInvalidate();
					return true;
				}
			}
			defaultModule.addBlock(b1);
				
			//only add bus2 if bus1 didn't hook up to a component, and it's bigger than 1 pixel
			Bus b2=new Bus(b1,defaultModule);
			b2.place(tempbus2.xcoor, tempbus2.ycoor, tempbus2.xcoor2, tempbus2.ycoor2, b1.number, 0);
			if (b2.xcoor==b2.xcoor2 && b2.ycoor==b2.ycoor2)
			{
				clearBusAction();
				postInvalidate();
				return false;
			}
			
			//look for a block, then a bus, for bus2 to connect to
			for (Block bl:defaultModule.blocks)
			{
				if (bl.getXEntrance(b2.xcoor2,b2.ycoor2)!=-1 && bl.getYEntrance(b2.xcoor2,b2.ycoor2)!=-1)
				{
					b2.output=bl.number;
					b2.xcoor2=bl.getXEntrance(b2.xcoor2,b2.ycoor2);
					b2.ycoor2=bl.getYEntrance(b2.xcoor2,b2.ycoor2);
					clearBusAction();
					if (b2.xcoor!=b2.xcoor2 && b2.ycoor!=b2.ycoor2)
						{
						postInvalidate();
						return false;
						}
					defaultModule.addBlock(b2);
					postInvalidate();
					return true;
				}
			}
			for (Bus bu:defaultModule.buses)
			{
				//only source if it's an orphan bus
				if (bu.input!=0) continue;
				
				if (bu.getXEntrance(b2.xcoor2,b2.ycoor2)!=-1 && bu.getYEntrance(b2.xcoor2,b2.ycoor2)!=-1)
				{
					b2.output=bu.number;
					b2.xcoor2=bu.getXEntrance(b2.xcoor2,b2.ycoor2);
					b2.ycoor2=bu.getYEntrance(b2.xcoor2,b2.ycoor2);
					clearBusAction();
					if (b2.xcoor!=b2.xcoor2 && b2.ycoor!=b2.ycoor2)
						{
						postInvalidate();
						return false;
						}
					defaultModule.addBlock(b2);
					bu.input=b2.number;
					postInvalidate();
					return true;
				}
			}
			//bus 2 goes nowhere, but add it anyway
			defaultModule.addBlock(b2);
			clearBusAction();
			postInvalidate();
			return true;
		}
		return false;		
	}

	private boolean trySelectBlock(MotionEvent arg0)
	{
		boolean retval=false;
		if (action.equals(""))
		{			
			//default action: alter selection state of a block
			for (Block b:defaultModule.blocks)
			{
				if (b.doSelect(getX(arg0), getY(arg0)) && !b.selected)
				{
					b.select();
					retval=true;
				}
					
				else if (b.doSelect(getX(arg0), getY(arg0)) && b.selected)
				{
					b.unselect();
					retval=true;
				}
				
				//if it's an input pin, toggle value
				if (b.doSelect(getX(arg0), getY(arg0)) && isSimulating && b.type.equals("input pin"))
				{
					b.setValue(b.getValue()+1);
					propagateAll();
					retval=true;
				}
			}
			for (Bus b:defaultModule.buses)
			{
				if (b.doSelect(getX(arg0), getY(arg0)) && !b.selected)
				{
					b.select();
					retval=true;
				}
				else if (b.doSelect(getX(arg0), getY(arg0)) && b.selected)
				{
					retval=true;
					b.unselect();
				}
			}
		}
		if (retval)
			postInvalidate();
		return retval;
	}
		public int getX(MotionEvent e)
	{
		int x=(int)((e.getX()-xshift)/scaling);
		x-=x%gridsize;
		return x;
	}
	public int getY(MotionEvent e)
	{
		int y=(int)((e.getY()-yshift)/scaling);
		y-=y%gridsize;
		return y;
	}

	
	public class Bus extends Part
	{
		public int input;
		public int output;
		public boolean isHorizontal;
		private DatapathModule module;
		
		public Bus(Bus b)
		{
			module=b.module;
			output=b.output; input=b.input; bits=b.bits;
			xcoor=b.xcoor; ycoor=b.ycoor; xcoor2=b.xcoor2; ycoor2=b.ycoor2;
			isHorizontal=b.isHorizontal;
			description=b.description;
			name=b.name;
			type=b.type;
		}
		public Bus(Block inputblock, DatapathModule module)
		{
			this.module=module;
			output=0;
			input=inputblock.number;
			bits=inputblock.bits;
			type="bus";
		}
		public Bus(Bus inputblock, DatapathModule module)
		{
			this.module=module;
			output=0;
			input=inputblock.number;
			bits=inputblock.bits;
			type="bus";
		}
		public Bus(int bits, DatapathModule module)
		{
			this.module=module;
			output=0;
			input=0;
			this.bits=bits;
			type="bus";
		}
		public long getValue()
		{
			return value&((long)Math.pow(2,bits)-1l);
		}
		public void setValue(long val)
		{
			this.value=val&((long)Math.pow(2,bits)-1l);
		}
		public Part[] getInputBlocks()
		{
			Part[] blist=new Part[1];
			blist[0]=module.getPart(input);
			return blist;
		}
		public String getErrorString()
		{
			for (ErrorEntry e:errorlog)
				if (e.number==number)
					return "bus "+e.number+": "+e.error;
			return "";
		}
		public void doPropagate()
		{
			if (input==0)
				error("no input to bus");

			for (Block b:module.blocks)
			{
				if (input==b.number && !b.type.equals("splitter") && !b.type.equals("module") && !b.type.equals("decoder"))
				{
					if (b.getValue()!=value)
						highlighted=true;
					setValue(b.getValue());
				}
			}
			for (Bus b:module.buses)
			{
				if (input==b.number)
				{
					if (b.getValue()!=value)
						highlighted=true;
					setValue(b.getValue());
				}
			}
		}
		private void error(String message)
		{
			System.out.println("Error in bus "+number+": "+message);
//			System.exit(0);
		}
		public int getXExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return x;
			if (xcoor==xcoor2)
				return xcoor;
			return -1;
		}
		public int getYExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return ycoor;
			if (xcoor==xcoor2)
				return y;
			return -1;
		}
		public boolean doSelect(int x, int y)
		{
			int precision=5;
			if (xcoor==xcoor2 && (x-xcoor)>=-precision && (x-xcoor)<=precision && ((y>=ycoor && y<=ycoor2)||(y>=ycoor2 && y<=ycoor))) return true;
			if (ycoor==ycoor2 && (y-ycoor)>=-precision && (y-ycoor)<=precision && ((x>=xcoor && x<=xcoor2)||(x>=xcoor2 && x<=xcoor))) return true;
			return false;
		}
		public int getXEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return x;
			if (xcoor==xcoor2)
				return xcoor;
			return -1;
		}
		public int getYEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return ycoor;
			if (xcoor==xcoor2)
				return y;
			return -1;
		}
		
		private void setSelectedColor(Paint g)
		{
			if (selected)
				g.setColor(Color.RED);
			else if (highlighted)
				g.setColor(Color.argb(255,255,50,0));
			else
				g.setColor(Color.BLACK);
		}
		private void drawLine(Canvas g, int a, int b, int c, int d, Paint p)
		{
			g.drawLine((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling+xshift),(int)(d*scaling+yshift),p);
		}
		private void drawString(Canvas g, String s, int a, int b, Paint p)
		{
			g.drawText(s,(int)(a*scaling+xshift),(int)(b*scaling+yshift),p);
		}
		public void draw(Canvas g)
		{
			Paint p=new Paint();
			setSelectedColor(p);
			p.setStrokeWidth((float) 1.5);
			drawLine(g,xcoor,ycoor,xcoor2,ycoor2,p);
			if (xcoor==xcoor2 && ycoor2>ycoor)
			{
				drawLine(g,xcoor-1,ycoor2-1,xcoor+1,ycoor2-1,p);
				drawLine(g,xcoor-2,ycoor2-2,xcoor+2,ycoor2-2,p);
				drawLine(g,xcoor-2,ycoor2-2,xcoor,ycoor2,p);
				drawLine(g,xcoor+2,ycoor2-2,xcoor,ycoor2,p);
			}
			else if (xcoor==xcoor2 && ycoor2<ycoor)
			{
				drawLine(g,xcoor-1,ycoor2+1,xcoor+1,ycoor2+1,p);
				drawLine(g,xcoor-2,ycoor2+2,xcoor+2,ycoor2+2,p);
				drawLine(g,xcoor-2,ycoor2+2,xcoor,ycoor2,p);
				drawLine(g,xcoor+2,ycoor2+2,xcoor,ycoor2,p);
			}
			else if (ycoor==ycoor2 && xcoor2>xcoor)
			{
				drawLine(g,xcoor2-1,ycoor2-1,xcoor2-1,ycoor2+1,p);
				drawLine(g,xcoor2-2,ycoor2-2,xcoor2-2,ycoor2+2,p);
				drawLine(g,xcoor2-2,ycoor2-2,xcoor2,ycoor2,p);
				drawLine(g,xcoor2-2,ycoor2+2,xcoor2,ycoor2,p);
			}
			else if (ycoor==ycoor2 && xcoor2<xcoor)
			{
				drawLine(g,xcoor2+1,ycoor2-1,xcoor2+1,ycoor2+1,p);
				drawLine(g,xcoor2+2,ycoor2-2,xcoor2+2,ycoor2+2,p);
				drawLine(g,xcoor2+2,ycoor2-2,xcoor2,ycoor2,p);
				drawLine(g,xcoor2+2,ycoor2+2,xcoor2,ycoor2,p);
			}
			if (!isHorizontal)
			{
				drawLine(g,xcoor-3,(ycoor+ycoor2)/2+3,xcoor+3,(ycoor+ycoor2)/2-3,p);
				drawString(g,""+bits,xcoor+1,(ycoor+ycoor2)/2+3,p);
				drawString(g,getErrorString(),xcoor+1,(ycoor+ycoor2)/2+9,p);
			}
			else
			{
				drawLine(g,(xcoor+xcoor2)/2-3,ycoor+3,(xcoor+xcoor2)/2+3,ycoor-3,p);
				drawString(g,""+bits,(xcoor+xcoor2)/2+3,ycoor-1,p);
				drawString(g,getErrorString(),(xcoor+xcoor2)/2+6,ycoor-1,p);
			}
		}		
		public void place(int x1, int y1, int x2, int y2, int entb, int exb)
		{
			xcoor=x1;
			ycoor=y1;
			xcoor2=x2;
			ycoor2=y2;
			input=entb;
			output=exb;
			if (xcoor==xcoor2)
				isHorizontal=false;
			else
				isHorizontal=true;
		}
		public String getXML()
		{
			String xml = "<bus>\n<number>"+number+"</number>\n<name>"+name+"</name>\n<bits>"+bits+"</bits>\n<xcoordinate>"+xcoor+"</xcoordinate>\n<ycoordinate>"+ycoor+"</ycoordinate>\n";
				xml+="<xcoordinate2>"+xcoor2+"</xcoordinate2>\n<ycoordinate2>"+ycoor2+"</ycoordinate2>\n"+"<description>"+description+"</description>\n";
				xml+="<entry>"+input+"</entry>\n";
				xml+="<exit>"+output+"</exit>\n";
			xml+="</bus>\n";
			return xml;
		}
		public void unselect()
		{
			selected=false;
//			if (modificationcomponent!=null)
//				modificationcomponent.dispose();
		}
		
		public void edit()
		{
			//TODO: edit here
//			modificationcomponent=new ModificationComponent(-1,number);			
		}
		
		public void select()
		{
			selected=true;
		}
		public boolean verify()
		{
			//all buses have a valid input and output
			if (input==0 || (module.getBlock(input)==null && module.getBus(input)==null))
			{
				errorlog.add(new ErrorEntry(number,"nobody is sourcing this bus"));
				return false;
			}
			if (output!=0 && module.getBlock(output)==null && module.getBus(output)==null) 
			{
				errorlog.add(new ErrorEntry(number,"the bus claims to be sourcing component "+output+", but "+output+" doesn't exist"));
				return false;
			}
			if (input==output)
			{
				errorlog.add(new ErrorEntry(number,"infinite loop"));
				return false;
			}
			if (output==0)
			{
			for (Bus b:module.buses)
				{
					if (b.input==number)
						return true;
				}
				errorlog.add(new ErrorEntry(number,"the bus doesn't source any component"));
				return false;
			}
			return true;
		}
		public void fix()
		{
			if (input!=0 && module.getBlock(input)!=null && !module.getBlock(input).type.equals("splitter")&& !module.getBlock(input).type.equals("decoder"))
				bits=module.getBlock(input).bits;
			if (input!=0 && module.getBus(input)!=null)
				bits=module.getBus(input).bits;
		}
	}
	//Block models all datapath components except Buses
	public class Block extends Part
	{
		public static final int MAX_BUSES_PER_BLOCK=32;

		public static final int XSIZE=40,YSIZE=30;

		private Hashtable<Integer,Long> regfilevalue=new Hashtable<Integer,Long>();
		public Hashtable<Integer,String> bus = new Hashtable<Integer,String>();

		//will this be clocked on the next cycle?
		public boolean clockSetting=false;
		public String operationSetting="";
		
		protected DatapathModule module;
		
		//photocopy the block
		public Block(Block b)
		{
			module=b.module;
			xcoor=b.xcoor; ycoor=b.ycoor; xcoor2=b.xcoor2; ycoor2=b.ycoor2;
			bits=b.bits;
			name=b.name;
			type=b.type;
			description=b.description;
			if (b.regfilevalue!=null)
				regfilevalue=(Hashtable<Integer, Long>) b.regfilevalue.clone();
			if (b.bus!=null)
				bus=(Hashtable<Integer, String>) b.bus.clone();
		}
		
		public Block(String type, int bits, DatapathModule module)
		{
			this.module=module;
			
			xcoor=-1;
			ycoor=-1;

			this.type=type;

			//memory and ports get their own name by default
			if (type.equals("memory"))
			{
				name="memory";
			}
			else if (type.equals("ports"))
			{
				name="ports";
			}
			
			//some units can only source 1 bit
			if (type.equals("flag")||type.equals("combinational-less-than")||type.equals("combinational-equal-to"))
				bits=1;
			this.bits=bits;
		}
		
		public void unselect()
		{
			selected=false;
//			if (modificationcomponent!=null)
//				modificationcomponent.dispose();
		}
		
		public int getBits(int xcoor)
		{
			if (type.equals("decoder"))
				return 1;
			return bits;
		}
		
		public void edit()
		{
			//TODO: edit here
//			modificationcomponent=new ModificationComponent(number,-1);			
		}
		
		public void select()
		{
			selected=true;
		}
		
		//return an array of all buses providing index inputs (horizontal) to the block
		public Bus[] getAddressInputBlock()
		{
			ArrayList<Bus> addressbus=new ArrayList<Bus>();
			for (Bus b:module.buses)
			{
				if (b.output==number && b.isHorizontal)
				{
					addressbus.add(b);
				}
			}
			return addressbus.toArray(new Bus[0]);
		}
		//return an array of all buses providing data inputs (vertical)
		public Bus[] getDataInputBlock()
		{
			ArrayList<Bus> databus=new ArrayList<Bus>();
			for (Bus b:module.buses)
			{
				if (b.output==number && !b.isHorizontal)
				{
					databus.add(b);
				}
			}
			return databus.toArray(new Bus[0]);
		}
		//return an array of all output buses
		public Bus[] getDataOutputBlock()
		{
			ArrayList<Bus> databus=new ArrayList<Bus>();
			for (Bus b:module.buses)
			{
				if (b.input==number)
				{
					databus.add(b);
				}
			}
			return databus.toArray(new Bus[0]);
		}

		//return an array of all input buses (basically address+data)
		public Part[] getInputBlocks()
		{
			ArrayList<Part> blist=new ArrayList<Part>();
			for (Bus b:module.buses)
				if (b.output==number && b.ycoor2==ycoor)
					blist.add(b);
			return blist.toArray(new Part[0]);
		}

		//if another bus is connected to a joiner input, increase the joiner's bits 
		public void updateJoinerBits()
		{
			bits=0;
			Bus[] b=getDataInputBlock();
			for (int i=0; i<b.length; i++)
				bits+=b[i].bits;
			postInvalidate();
		}
		
		//called to set the data value of the component
		//regfiles, memory, and ports must be indexed; lookup tables can't be changed
		public void setValue(long val)
		{
			if (type.equals("register file"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for reg file");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				this.regfilevalue.put(new Integer(addr),new Long(val&((long)Math.pow(2,bits)-1l)));
			}
			else if (type.equals("memory"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for memory");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
//				for (int i=0; i<bits; i+=8)
//					computer.physicalMemory.setByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("ports"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for port");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
//				for (int i=0; i<bits; i+=8)
//					computer.ioports.ioPortWriteByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("lookup table")){}
			else
				this.value=val&((long)Math.pow(2,bits)-1l);
		}
		//called to set a value of an indexed component manually
		public void setValue(int addr, long val)
		{
			if (type.equals("register file"))
			{
				this.regfilevalue.put(new Integer(addr),new Long(val&((long)Math.pow(2,bits)-1l)));
			}
			else if (type.equals("memory"))
			{
//				for (int i=0; i<bits; i+=8)
//					computer.physicalMemory.setByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("ports"))
			{
//				for (int i=0; i<bits; i+=8)
//					computer.ioports.ioPortWriteByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("lookup table"))
			{
				this.regfilevalue.put(new Integer(addr),new Long(val&((long)Math.pow(2,bits)-1l)));
			}
			else
				this.value=val&((long)Math.pow(2,bits)-1l);			
		}
		public long getValue(int addr)
		{
			if (type.equals("register file")||type.equals("lookup table"))
			{
				if (regfilevalue.get(new Integer(addr))==null) return 0;
				return ((Long)regfilevalue.get(new Integer(addr))).longValue()&((long)Math.pow(2,bits)-1l);
			}			
			else if (type.equals("memory"))
			{
				long val=0;
//				for (int i=0; i<bits; i+=8)
//					val+=(long)computer.physicalMemory.getByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("ports"))
			{
				long val=0;
//				for (int i=0; i<bits; i+=8)
//					val+=(long)computer.ioports.ioPortReadByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);				
			}
			else
				return 0;
		}
		public long getValue()
		{
			if (type.equals("register file")||type.equals("lookup table"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for reg file");
					return 0;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				if (regfilevalue.get(new Integer(addr))==null) return 0;
				return ((Long)regfilevalue.get(new Integer(addr))).longValue()&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("memory"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for memory");
					return 0;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				long val=0;
//				for (int i=0; i<bits; i+=8)
//					val+=(long)computer.physicalMemory.getByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("ports"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for ports");
					return 0;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				long val=0;
//				for (int i=0; i<bits; i+=8)
//					val+=(long)computer.ioports.ioPortReadByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("constant"))
			{
				return Long.parseLong(name,16);
			}
			else
				return value&((long)Math.pow(2,bits)-1l);
		}
		public void resetClock()
		{
			clockSetting=false;
		}
		//called by the processor unit on each clock cycle
		public void doClock()
		{
			if (!clockSetting) return;
			Bus[] input=getDataInputBlock();
/*			if (input.length==0 || input.length>=2)
			{
				error("clocking register without a single input bus");
				setValue(0);
				return;
			}*/
			if (type.equals("register")||type.equals("flag"))
			{
				highlighted=true;
				setValue(input[0].getValue());
			}
			else if (type.equals("register file") || type.equals("ports") || type.equals("memory"))
			{
				highlighted=true;
				setValue(input[0].getValue());
			}
		}
		//go through each combinational unit and route data through it
		public void doPropagate()
		{
			if (type.equals("adder")||type.equals("combinational-adder"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("adder needs at least one input");
					return;
				}
				long v=0;
				for (int i=0; i<bs.length; i++)
					v+=bs[i].getValue();
				setValue(v&0xffffffffffffffffl);					
			}
			else  if (type.equals("combinational-and"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("and needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v&=bs[i].getValue();
				setValue(v);					
			}
			else  if (type.equals("combinational-or"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("or needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v|=bs[i].getValue();
				setValue(v);					
			}
			else  if (type.equals("combinational-nor"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("nor needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v|=bs[i].getValue();
				setValue(~v);					
			}
			else  if (type.equals("combinational-nand"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("nand needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v&=bs[i].getValue();
				setValue(~v);					
			}
			else  if (type.equals("combinational-xor"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("xor needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v^=bs[i].getValue();
				setValue(v);					
			}
			else  if (type.equals("combinational-not"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("not needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue(~v);					
			}
			else if (type.equals("combinational-negate"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("negate needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue(-v);					
			}
			else  if (type.equals("combinational-increment"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("increment needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue((v+1)&0xffffffffffffffffl);					
			}
			else  if (type.equals("combinational-decrement"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("decrement needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue((v-1)&0xffffffffffffffffl);					
			}
			else  if (type.equals("combinational-less-than"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=2)
				{
					error("less-than needs two inputs");
					return;
				}
				setValue(bs[0].getValue()<bs[1].getValue()?1l:0);					
			}
			else  if (type.equals("combinational-equal-to"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=2)
				{
					error("equal-to needs two inputs");
					return;
				}
				setValue(bs[0].getValue()==bs[1].getValue()?1l:0);					
			}
			else  if (type.equals("combinational-shift-right"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1 && bs.length!=2)
				{
					error("shift needs one or two inputs");
					return;
				}
				if (bs.length==1)
					setValue(bs[0].getValue()>>>1);
				else
					setValue(bs[0].getValue()>>>bs[1].getValue());
			}
			else  if (type.equals("combinational-shift-left"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1 && bs.length!=2)
				{
					error("shift needs one or two inputs");
					return;
				}
				if (bs.length==1)
					setValue(bs[0].getValue()<<1);
				else
					setValue(bs[0].getValue()<<bs[1].getValue());
			}
			else if (type.equals("ALU"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("ALU needs at least one input bus");
					return;
				}
				long v1,v2;
				if (bs[0].xcoor<bs[1].xcoor)
				{
					v1=bs[0].getValue();
					v2=bs[1].getValue();
				}
				else
				{
					v1=bs[1].getValue();
					v2=bs[0].getValue();
				}
				if (operationSetting.equals("+"))
					setValue(v1+v2);
				else if (operationSetting.equals("-"))
					setValue(v1-v2);
				else if (operationSetting.equals("*"))
					setValue(v1*v2);
				else if (operationSetting.equals("/"))
					setValue(v1/v2);
				else if (operationSetting.equals("AND"))
					setValue(v1&v2);
				else if (operationSetting.equals("OR"))
					setValue(v1|v2);
				else if (operationSetting.equals("XOR"))
					setValue(v1^v2);
				else if (operationSetting.equals("XNOR"))
					setValue(~(v1^v2));
				else if (operationSetting.equals("NAND"))
					setValue(~(v1&v2));
				else if (operationSetting.equals("NOR"))
					setValue(~(v1|v2));
				else if (operationSetting.equals("NOT"))
					setValue(~v1);
				else if (operationSetting.equals("<<"))
					setValue(v1<<v2);
				else if (operationSetting.equals(">>"))
					setValue(v1>>>v2);
				else if (operationSetting.equals("=="))
					setValue(v1==v2? 1l:0);
				else if (operationSetting.equals("==0?"))
					setValue(v1==0? 1l:0);
				else if (operationSetting.equals("!=0?"))
					setValue(v1==0? 0:1l);
				else if (operationSetting.equals("!="))
					setValue(v1!=v2? 1l:0);
				else if (operationSetting.equals("<"))
					setValue(v1<v2? 1l:0);
				else if (operationSetting.equals("<="))
					setValue(v1<=v2? 1l:0);
				else if (operationSetting.equals(">"))
					setValue(v1>v2? 1l:0);
				else if (operationSetting.equals(">="))
					setValue(v1>=v2? 1l:0);
				else if (operationSetting.equals("+1"))
					setValue(v1+1l);
				else if (operationSetting.equals("-1"))
					setValue(v1-1l);
				else if (operationSetting.equals("0"))
					setValue(0);
				else if (operationSetting.equals("IN1"))
					setValue(v1);
				else if (operationSetting.equals("IN2"))
					setValue(v2);
				else if (operationSetting.equals("NOP"))
					setValue(v1);
			}
			else if (type.equals("multiplexor")&&getAddressInputBlock().length==0)
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("mux needs an input");
					return;
				}
				for (int i=0; i<bs.length; i++)
				{
					for (int j=0; j<bs.length-1; j++)
					{
						if (bs[j].xcoor>bs[j+1].xcoor)
						{
							Bus tmpblock=bs[j];
							bs[j]=bs[j+1];
							bs[j+1]=tmpblock;
						}
					}
				}
				if (operationSetting.equals(""))
					setValue(bs[0].getValue());
				else
					setValue(bs[Integer.parseInt(operationSetting,16)].getValue());
			}
			else if (type.equals("data_multiplexor") || (type.equals("multiplexor")&&getAddressInputBlock().length!=0))
			{
				if (getAddressInputBlock().length==0)
				{
					error("data mux needs an address input");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
//				System.out.println("propagate "+type+" "+number+" "+addr);
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("mux needs an input");
					return;
				}
				for (int i=0; i<bs.length; i++)
				{
					for (int j=0; j<bs.length-1; j++)
					{
						if (bs[j].xcoor>bs[j+1].xcoor)
						{
							Bus tmpblock=bs[j];
							bs[j]=bs[j+1];
							bs[j+1]=tmpblock;
						}
					}
				}
				if (addr>=bs.length) return;
				setValue(bs[addr].getValue());
			}
			else if (type.equals("control") || type.equals("extender"))
			{
				Bus[] input=getDataInputBlock();
				if (input.length==0) 
				{
					error("control/extender needs an input");
					return;
				}
				setValue(input[0].getValue());
			}
			else if (type.equals("output pin"))
			{
				Bus[] input=getDataInputBlock();
				if (input.length==0) 
				{
					error("output pin needs an input");
					return;
				}
				setValue(input[0].getValue());
			}
			else if (type.equals("decoder"))
			{
				setValue(((Bus)(getInputBlocks()[0])).getValue());
				for (int i=0; i<getDataOutputBlock().length; i++)
				{
					int buspin=(int)Math.pow(2,bits)-(getDataOutputBlock()[i].xcoor-xcoor)/decodersize-1;
					if (buspin==getValue())
						getDataOutputBlock()[i].setValue(1);
					else
						getDataOutputBlock()[i].setValue(0);
				}
			}
			else if (type.equals("splitter"))
			{
				if (getInputBlocks().length==0) 
				{
					error("splitter needs an input");
					return;
				}
				setValue(((Bus)(getInputBlocks()[0])).getValue());
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();

					for (Bus b:module.buses)
					{
						if (b.number==i)
						{
							String busstring=(String)bus.get(new Integer(i));
							int b1=Integer.parseInt(busstring.substring(0,busstring.indexOf(":")));
							int b2=Integer.parseInt(busstring.substring(busstring.indexOf(":")+1,busstring.length()));
							long v=getValue();
							v=v>>>(long)b2;
							v=v&(long)(Math.pow(2,b1-b2+1)-1);
							b.setValue(v);
						}
					}
				}
			}
			else if (type.equals("joiner"))
			{
				Bus[] input=getDataInputBlock();
				if (input.length<1)
				{
					error("joiner needs an input");
					return;
				}
				for (int i=0; i<input.length; i++)
				{
					for (int j=0; j<input.length-1; j++)
					{
						if (input[j].xcoor>input[j+1].xcoor)
						{
							Bus tmpblock=input[j];
							input[j]=input[j+1];
							input[j+1]=tmpblock;
						}
					}
				}
				long v=0;
				v=input[0].getValue();
				for (int i=1; i<input.length; i++)
				{
					v=v<<input[i].bits;
					v=v|input[i].getValue();
				}
				setValue(v);
			}
			else if (type.equals("lookup table"))
			{
				value=getValue();
			}
			else if (type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("constant"))
			{
				value=getValue();
			}
			else if (type.equals("register")||type.equals("flag"))
			{
				if (getAddressInputBlock().length>0)
					clockSetting=getAddressInputBlock()[0].getValue()!=0;
			}
		}

		private void error(String message)
		{
			System.out.println("Error in block "+number+": "+message);
//			System.exit(0);
		}

		//place the component on the graphical layout
		public void place(int x, int y, int x2, int y2)
		{
			if (x2==0 && y2==0)
				place(x,y);
			else
			{
				xcoor=x; ycoor=y; xcoor2=x2; ycoor2=y2;
			}
		}
		//determines the image size of the component
		public void place(int x, int y)
		{
			xcoor=x;
			ycoor=y;
			if (type.equals("register")||type.equals("lookup table")||type.equals("ALU")||type.equals("adder")||type.equals("register file")||type.equals("memory")||type.equals("ports"))
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE;
			}
			else if (type.equals("decoder"))
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE/2;
				if (Math.pow(2, bits)*decodersize>XSIZE)
					xcoor2=x+(int)Math.pow(2, bits)*decodersize;
			}
			else if (type.equals("flag")||type.equals("constant"))
			{
				xcoor2=x+XSIZE/2;
				ycoor2=y+YSIZE/2;
			}
			else if (type.equals("splitter")||type.equals("joiner"))
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE/3;
			}
			else if (type.equals("input pin")||type.equals("output pin"))
			{
				xcoor2=x+XSIZE/3;
				ycoor2=y+YSIZE/3;
			}
			else if (type.equals("combinational-not")||type.equals("combinational-negate"))
			{
				xcoor2=x+XSIZE/2;
				ycoor2=y+YSIZE/2;				
			}
			else
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE/2;
			}
		}
		public void delete()
		{
			module.blocks.remove(this);
		}
		public String getErrorString()
		{
			for (ErrorEntry e:errorlog)
				if (e.number==number)
					return "block "+e.number+": "+e.error;
			return "";
		}
		//does the block appear at (x,y)?
		public boolean doSelect(int x, int y)
		{
			if (x>=xcoor && y>=ycoor && x<=xcoor2 && y<=ycoor2) return true;
			return false;
		}
		//if the user clicked at x,y, does that connect to an exit line for the block?
		//if so, return the precise x coordinate for the block's exit
		public int getXExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("register")||type.equals("memory")||type.equals("ports")||type.equals("register file")||type.equals("lookup table")||type.equals("ALU")||type.equals("")||type.equals("data_multiplexor")||type.equals("multiplexor")||type.equals("joiner")||type.equals("inhibitor")||type.equals("extender")||type.equals("lookup table"))
				return (xcoor+xcoor2)/2;
			if (type.length()>=14 && type.substring(0,14).equals("combinational-"))
				return (xcoor+xcoor2)/2;
			if (type.equals("flag")||type.equals("constant"))
				return (xcoor+xcoor2)/2;
			if (type.equals("decoder")||type.equals("splitter"))
				return x;
			if (type.equals("input pin"))
				return (xcoor+xcoor2)/2;
			return -1;
		}
		//if the user clicked at x,y, does that connect to an exit line for the block?
		//if so, return the precise y coordinate for the block's exit
		//in almost all cases, this is simply the bottom of the block
		public int getYExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			return ycoor2;
		}
		//if the user clicked at x,y, does that connect to an input line for the block?
		//if so, return the precise x coordinate for the block's input
		//some components have both address and data inputs which appear in different places
		public int getXEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("lookup table")||type.equals("extender")||type.equals("decoder")||type.equals("inhibitor")||type.equals("splitter")||type.equals("joiner")||type.equals("control"))
				return x;
			if ((type.equals("ALU")||type.equals("adder"))&&x>xcoor&&x<xcoor+(xcoor2-xcoor)/2-(xcoor2-xcoor)/5)
				return x;
			if ((type.equals("ALU")||type.equals("adder"))&&x<xcoor2&&x>xcoor+(xcoor2-xcoor)/2+(xcoor2-xcoor)/5)
				return x;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor"))&&y<ycoor+(ycoor2-ycoor)/4)
				return x;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("lookup table"))&&y>=ycoor+(ycoor2-ycoor)/3&&x<xcoor+(xcoor2-xcoor)/3)
				return xcoor;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("lookup table"))&&y>=ycoor+(ycoor2-ycoor)/3&&x>xcoor+(xcoor+xcoor2)/2-(xcoor2-xcoor)/3)
				return xcoor2;
			if (type.equals("output pin"))
				return x;
			if (((type.length()>=14 && type.substring(0,14).equals("combinational-"))))
				return x;
			return -1;
		}
		//if the user clicked at x,y, does that connect to an input line for the block?
		//if so, return the precise y coordinate for the block's input
		//some components have both address and data inputs which appear in different places
		public int getYEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("extender")||type.equals("decoder")||type.equals("inhibitor")||type.equals("splitter")||type.equals("joiner")||type.equals("ALU")||type.equals("adder")||type.equals("control"))
				return ycoor;
			if (((type.length()>=14 && type.substring(0,14).equals("combinational-"))))
				return ycoor;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor"))&&y<=ycoor+(ycoor2-ycoor)/3)
				return ycoor;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("lookup table"))&&y>=ycoor+(ycoor2-ycoor)/3)
				return y;
			if (type.equals("output pin"))
				return ycoor;
			return -1;
		}

		//the block's edge is either BLACK (normal), RED (selected), or orangeish (highlighted)
		protected void setSelectedColor(Paint p)
		{
			if (selected)
				p.setColor(Color.RED);
			else if (highlighted)
				p.setColor(Color.YELLOW);
			else
				p.setColor(Color.BLACK);
		}
		//various methods for drawing components
		//all of these are scaled so that you can zoom in or out
		protected void drawRect(Canvas g, int a, int b, int c, int d, Paint p)
		{
			g.drawRect((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling)+(int)(a*scaling+xshift),(int)(d*scaling)+(int)(b*scaling+yshift),p);
		}
		protected void fillRect(Canvas g, int a, int b, int c, int d, Paint p)
		{
			drawRect(g,a+1,b+1,c-2,d-2,p);
		}
//		protected void fillRect(Graphics g, int a, int b, int c, int d)
//		{
//			g.fillRect((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling),(int)(d*scaling));
//		}
		protected void drawLine(Canvas g, int a, int b, int c, int d, Paint p)
		{
			g.drawLine((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling+xshift),(int)(d*scaling+yshift),p);
		}
		private void drawOval(Canvas g, int a, int b, int c, int d, Paint p)
		{
			g.drawOval(new RectF((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(a*scaling+xshift)+(int)(c*scaling),(int)(b*scaling+yshift)+(int)(d*scaling)),p);
		}
		protected void fillOval(Canvas g, int a, int b, int c, int d, Paint p)
		{
			drawOval(g,a+1,b+1,c-2,d-2,p);
		}
//		private void fillOval(Graphics g, int a, int b, int c, int d)
//		{
//			g.fillOval((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling),(int)(d*scaling));
//		}
		private void drawArc(Canvas g, int a, int b, int c, int d, int e, int f, Paint p)
		{
			g.drawArc(new RectF((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(a*scaling+xshift)+(int)(c*scaling),(int)(b*scaling+yshift)+(int)(d*scaling)),e,-f,false,p);
		}
		protected void drawString(Canvas g, String s, int a, int b, Paint p)
		{
			g.drawText(s,(int)(a*scaling+xshift),(int)(b*scaling+yshift),p);
		}
		public void draw(Canvas g)
		{
			Paint p=new Paint();
			//draw the block's image
			if (type.equals("register"))
			{
				setSelectedColor(p);
				drawRect(g,xcoor,ycoor,xcoor2-xcoor,ycoor2-ycoor,p);
				p.setColor(Color.argb(255,200,200,255));
				fillRect(g,xcoor+1,ycoor+1,xcoor2-xcoor-2,ycoor2-ycoor-2,p);
			}
			else if (type.equals("label"))
			{
				setSelectedColor(p);
				drawRect(g,xcoor,ycoor,xcoor2-xcoor,ycoor2-ycoor,p);
				p.setColor(Color.argb(255,189,183,107));
				fillRect(g,xcoor,ycoor,xcoor2-xcoor,ycoor2-ycoor,p);
				p.setColor(Color.WHITE);
				p.setTextSize((float)(8*scaling));
				drawString(g,name,xcoor+5,ycoor2-3,p);
			}
			else if (type.equals("flag"))
			{
				setSelectedColor(p);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
				p.setColor(Color.WHITE);
				fillRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
				//flag is up
				if (getValue()!=0)
				{
					//staff
					p.setColor(Color.BLACK);
					drawLine(g,xcoor+(xcoor2-xcoor)/3,ycoor+2,xcoor+(xcoor2-xcoor)/3,ycoor2-2,p);
					p.setColor(Color.RED);
					drawLine(g,xcoor+(xcoor2-xcoor)/3,ycoor+2,xcoor2-2,(ycoor+ycoor2)/2,p);
					drawLine(g,xcoor+(xcoor2-xcoor)/3,(ycoor+ycoor2)/2,xcoor2-2,(ycoor+ycoor2)/2,p);
				}
				//flag is down
				else
				{
					p.setColor(Color.BLACK);
					drawLine(g,xcoor+2,ycoor+(ycoor2-ycoor)/3,xcoor2-2,ycoor+(ycoor2-ycoor)/3,p);
					p.setColor(Color.RED);
					drawLine(g,xcoor2-2,ycoor+(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor2-2,p);
					drawLine(g,(xcoor+xcoor2)/2,ycoor+(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor2-2,p);
				}
			}
			else if (type.equals("lookup table"))
			{
				setSelectedColor(p);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
				p.setColor(Color.WHITE);
				fillRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
				setSelectedColor(p);
				drawLine(g,xcoor+5,ycoor+(ycoor2-ycoor)/4,xcoor+(xcoor2-xcoor)-5,ycoor+(ycoor2-ycoor)/4,p);
				drawLine(g,xcoor+(xcoor2-xcoor)/2,ycoor+(ycoor2-ycoor)/4,xcoor+(xcoor2-xcoor)/2,ycoor+(ycoor2-ycoor)-(ycoor2-ycoor)/4,p);
			}
			else if (type.equals("ALU")||type.equals("adder")||type.equals("combinational-adder")||type.equals("combinational-less-than")||type.equals("combinational-equal-to")||type.equals("combinational-shift-left")||type.equals("combinational-shift-right"))
			{
				setSelectedColor(p);
				int x=xcoor+(xcoor2-xcoor)/2,y=ycoor+(ycoor2-ycoor)/2;
				drawLine(g,x-(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x-(xcoor2-xcoor)/2+(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2,p);	//left
				drawLine(g,x+(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x+(xcoor2-xcoor)/2-(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2,p);	//right
				drawLine(g,x-(xcoor2-xcoor)/2+(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2,x+(xcoor2-xcoor)/2-(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2,p);	//bottom
				drawLine(g,x-(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2,x,y-(ycoor2-ycoor)/3,p);		//left notch
				drawLine(g,x+(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2,x,y-(ycoor2-ycoor)/3,p);		//right notch
				drawLine(g,x-(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x-(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2,p);	//left top
				drawLine(g,x+(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x+(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2,p);	//right top
			}
			else if (type.equals("memory")||type.equals("ports")||type.equals("register file"))
			{
				setSelectedColor(p);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
				if (type.equals("register file"))
					p.setColor(Color.argb(255,200,200,255));
				else if (type.equals("memory"))
					p.setColor(Color.argb(255,255,200,255));
				else if (type.equals("ports"))
					p.setColor(Color.argb(255,200,255,220));
				fillRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
				setSelectedColor(p);
				drawLine(g,xcoor,ycoor+(ycoor2-ycoor)/3,xcoor+(xcoor2-xcoor),ycoor+(ycoor2-ycoor)/3,p);
				drawLine(g,xcoor,ycoor+2*(ycoor2-ycoor)/3,xcoor+(xcoor2-xcoor),ycoor+2*(ycoor2-ycoor)/3,p);
			}
			else if (type.equals("extender")||type.equals("inhibitor"))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawOval(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
			}
			else if (type.equals("joiner"))
			{
				setSelectedColor(p);
				drawLine(g,xcoor,ycoor,(xcoor+xcoor2)/2,ycoor2,p);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor,p);
				drawLine(g,xcoor2,ycoor,(xcoor+xcoor2)/2,ycoor2,p);
			}
			else if (type.equals("splitter"))
			{
				setSelectedColor(p);
				drawLine(g,xcoor,ycoor2,(xcoor+xcoor2)/2,ycoor,p);
				drawLine(g,xcoor,ycoor2,xcoor2,ycoor2,p);
				drawLine(g,xcoor2,ycoor2,(xcoor+xcoor2)/2,ycoor,p);
			}
			else if (type.equals("multiplexor")||type.equals("data_multiplexor"))
			{
				setSelectedColor(p);
				drawLine(g,xcoor,ycoor,xcoor+(xcoor2-xcoor),ycoor,p);
				drawLine(g,xcoor+(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor),xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor),p);
				drawLine(g,xcoor,ycoor,xcoor+(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor),p);
				drawLine(g,xcoor+(xcoor2-xcoor),ycoor,xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor),p);
			}
			else if (type.equals("decoder"))
			{
				setSelectedColor(p);
				drawLine(g,xcoor,ycoor+(ycoor2-4-ycoor),xcoor+(xcoor2-xcoor),ycoor+(ycoor2-4-ycoor),p);
				drawLine(g,xcoor+(xcoor2-xcoor)/5,ycoor,xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor,p);
				drawLine(g,xcoor,ycoor+(ycoor2-4-ycoor),xcoor+(xcoor2-xcoor)/5,ycoor,p);
				drawLine(g,xcoor+(xcoor2-xcoor),ycoor+(ycoor2-4-ycoor),xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor,p);
				for (int i=0; i<Math.pow(2, bits); i++)
					drawLine(g,xcoor+i*decodersize,ycoor2,xcoor+i*decodersize,ycoor2-4,p);
			}
			else if (type.equals("constant"))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawOval(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
			}
			else if (type.equals("input pin"))
			{
				setSelectedColor(p);
				drawLine(g,(xcoor+xcoor2)/2,ycoor2-(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor2,p);
				drawOval(g,xcoor,ycoor,xcoor2-xcoor,2*(ycoor2-ycoor)/3,p);
				if (getValue()==0)
					p.setColor(Color.RED);
				else
					p.setColor(Color.GREEN);
				fillOval(g,xcoor,ycoor,xcoor2-xcoor,2*(ycoor2-ycoor)/3,p);
			}
			else if (type.equals("output pin"))
			{
				setSelectedColor(p);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor,p);
				drawLine(g,(xcoor+xcoor2)/2,ycoor+(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor,p);
				drawOval(g,xcoor,ycoor+(ycoor2-ycoor)/3,xcoor2-xcoor,2*(ycoor2-ycoor)/3,p);
				if (getValue()==0)
					p.setColor(Color.RED);
				else
					p.setColor(Color.GREEN);
				fillOval(g,xcoor,ycoor+(ycoor2-ycoor)/3,xcoor2-xcoor,2*(ycoor2-ycoor)/3,p);
			}
			else if (type.equals("combinational-and"))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawArc(g,xcoor,ycoor-(ycoor2-ycoor),(xcoor2-xcoor),(ycoor2-ycoor)*2,0,-180,p);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor,p);
			}
			else if (type.equals("combinational-nand"))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawArc(g,xcoor,ycoor-((ycoor2-4)-ycoor),(xcoor2-xcoor),((ycoor2-4)-ycoor)*2,0,-180,p);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor,p);
				drawOval(g,xcoor+(xcoor2-xcoor)/2-2,ycoor2-4,4,4,p);
			}
			else if (type.equals("combinational-nor"))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawArc(g,xcoor,ycoor-((ycoor2-4)-ycoor),(xcoor2-xcoor),((ycoor2-4)-ycoor)*2,0,-180,p);
				drawArc(g,xcoor,ycoor-2,(xcoor2-xcoor),4,0,-180,p);
				drawOval(g,xcoor+(xcoor2-xcoor)/2-2,ycoor2-4,4,4,p);
			}
			else if (type.equals("combinational-not"))
			{
				setSelectedColor(p);				
				p.setStyle(Style.STROKE);
				drawOval(g,xcoor+(xcoor2-xcoor)/2-2,ycoor2-4,4,4,p);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor,p);
				drawLine(g,xcoor,ycoor,(xcoor+xcoor2)/2,ycoor2-4,p);
				drawLine(g,(xcoor+xcoor2)/2,ycoor2-4,xcoor2,ycoor,p);
			}
			else if (type.equals("combinational-or"))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawArc(g,xcoor,ycoor-(ycoor2-ycoor),(xcoor2-xcoor),(ycoor2-ycoor)*2,0,-180,p);
				drawArc(g,xcoor,ycoor-2,(xcoor2-xcoor),4,0,-180,p);
			}
			else if (type.equals("combinational-xor"))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawArc(g,xcoor,ycoor-(ycoor2-ycoor-2),(xcoor2-xcoor),(ycoor2-ycoor-2)*2,0,-180,p);
				drawArc(g,xcoor,ycoor-2+2,(xcoor2-xcoor),4,0,-180,p);
				drawArc(g,xcoor,ycoor-2,(xcoor2-xcoor),4,0,-180,p);
			}
			else if (((type.length()>=14 && type.substring(0,14).equals("combinational-"))))
			{
				setSelectedColor(p);
				p.setStyle(Style.STROKE);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor),p);
				String subtype=type.substring(14);
			}
			p.setColor(Color.BLACK);
			p.setTextSize((int)(8*scaling));
			if (!name.equals(""))
			{
				if (type.equals("constant"))
					drawString(g,name,xcoor+5,ycoor+10,p);
				else if (!type.equals("label"))
					drawString(g,name,xcoor+5,ycoor2-4,p);
			}
			//label the number of bits
			p.setColor(Color.argb(255,0,150,0));
			if (!type.equals("bus")&&!type.equals("extender")&&!type.equals("joiner")&&!type.equals("constant")&&!type.equals("label"))
				drawString(g,""+bits,xcoor+1,ycoor-1,p);
			else if (type.equals("extender")||type.equals("joiner"))
				drawString(g,""+bits,xcoor+1,ycoor+(ycoor2-ycoor)+12,p);
			else if (type.equals("constant"))
				drawString(g,""+bits,xcoor+1,ycoor+(ycoor2-ycoor)+12,p);
			//each splitter output gets its own bit label
			if (type.equals("splitter"))
			{
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();
					for (Bus b:module.buses)
					{
						if (b.number==i)
							drawString(g,(String)bus.get(new Integer(i)),b.xcoor,ycoor+(ycoor2-ycoor)/3+12,p);
					}
				}
			}
			//label the current value
			if (isSimulating)
			{
				p=new Paint();
				if (type.equals("register")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("lookup table"))
				{
					p.setColor(Color.argb(255,0,150,0));
					drawRect(g,xcoor-7,ycoor+YSIZE+12-9,(bits/4+1)*7,12,p);
					p.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE+12,p);

					if (getAddressInputBlock().length>0)
					{
						p.setColor(Color.argb(255,100,100,0));
						drawRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12,p);
						p.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock()[0].getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2,p);
					}
				}
				else if (type.equals("adder")||type.equals("ALU"))
				{
					p.setColor(Color.argb(255,50,150,50));
					drawRect(g,xcoor-7,ycoor+YSIZE+12-9,(bits/4+1)*7,12,p);
					p.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE+12,p);
				}
				else if ((type.length()>=14 && type.substring(0,14).equals("combinational-")))
				{
					p.setColor(Color.argb(255,50,150,50));
					drawRect(g,xcoor-7,ycoor+YSIZE+12-9,(bits/4+1)*7,12,p);
					p.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE+12,p);					
				}
				else if (type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("joiner")||type.equals("control"))
				{
					p.setColor(Color.argb(255,50,150,50));
					drawRect(g,xcoor-7,ycoor+YSIZE/3+12-9,(bits/4+1)*7,12,p);
					p.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE/3+12,p);

					if (type.equals("data_multiplexor")||getAddressInputBlock().length!=0)
					{
						p.setColor(Color.argb(255,100,100,0));
						drawRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12,p);
						p.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock()[0].getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2,p);
					}
				}
				else if (type.equals("constant")||type.equals("flag")||type.equals("input pin")||type.equals("output pin"))
				{
					p.setColor(Color.argb(255,0,150,0));
					drawRect(g,xcoor-7,ycoor+YSIZE/2+12-9,(bits/4+1)*7,12,p);
					p.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE/2+12,p);
					if (getAddressInputBlock().length>0)
					{
						p.setColor(Color.argb(255,100,100,0));
						drawRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12,p);
						p.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock()[0].getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2,p);
					}
				}
				else if (type.equals("bus") && xcoor==xcoor2)
				{
					p.setColor(Color.argb(255,50,150,50));
					drawRect(g,xcoor-7,(ycoor+ycoor2)/2+12-9,(bits/4+1)*7,12,p);
					p.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,(ycoor+ycoor2)/2+12,p);
				}
				else if (type.equals("bus") && ycoor==ycoor2)
				{
					p.setColor(Color.argb(255,50,150,50));
					drawRect(g,(xcoor+xcoor2)/2-3,ycoor-14,(bits/4+1)*7,12,p);
					p.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),(xcoor+xcoor2)/2-3,ycoor-14+12,p);
				}
			}
			p.setColor(Color.RED);
			drawString(g,getErrorString(),xcoor+3,ycoor-1,p);				
		}
		public String getXML()
		{
			String xml = "<"+type+">\n<number>"+number+"</number>\n<name>"+name+"</name>\n<bits>"+bits+"</bits>\n<xcoordinate>"+xcoor+"</xcoordinate>\n<ycoordinate>"+ycoor+"</ycoordinate>\n";
				xml+="<xcoordinate2>"+xcoor2+"</xcoordinate2>\n<ycoordinate2>"+ycoor2+"</ycoordinate2>\n<description>"+description+"</description>\n";
			if (type.equals("splitter"))
			{
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();
					xml+="<line "+i+">"+(String)bus.get(new Integer(i))+"</line>\n";
				}
			}
			if (type.equals("lookup table"))
			{
				for (Enumeration e=regfilevalue.keys(); e.hasMoreElements();)
				{
					Integer a=(Integer)e.nextElement();
					Long v=regfilevalue.get(a);
					xml+="<value "+a.intValue()+">"+Long.toHexString(v.longValue())+"</value>\n";
				}
			}
			xml+="</"+type+">\n";
			return xml;
		}

		public String controlInputs()
		{
			if ((type.equals("register")||type.equals("flag"))&&getAddressInputBlock().length==0)
				return "1 clock "+name;
			else if (type.equals("register file")||type.equals("ports")||type.equals("memory"))
				return "1 clock "+name;
			else if (type.equals("ALU"))
				return "1 alu "+name;
			else if (type.equals("multiplexor")&&getAddressInputBlock().length==0)
			{
				int i=0;
				for (Bus b:module.buses)
				{
					if (b.output==number)
						i++;
				}
				return ""+i+" mux "+name;
			}
			return "";
		}

		public String controlOutputs()
		{
			if (type.equals("control"))
				return ""+bits+" "+name;
			else
				return "";
		}

		public boolean verify()
		{
			//one input bus, input bus has same width
			if (type.equals("register")||type.equals("flag"))
			{
				if (getDataInputBlock().length!=1)
				{
					errorlog.add(new ErrorEntry(number,"must have one and only one input"));
					return false;
				}
				if (getDataInputBlock()[0].bits!=bits)
				{
					errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
					return false;
				}
				if (getAddressInputBlock().length>1)
				{
					errorlog.add(new ErrorEntry(number,"can't have multiple clock enable inputs"));
					return false;
				}
				if (getAddressInputBlock().length==1 && getAddressInputBlock()[0].bits!=1) 
				{
					errorlog.add(new ErrorEntry(number,"clock enable input can only have 1 bit"));
					return false;
				}
				if (name.equals("")) 
					{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
					}
				return true;
			}
			//one input bus, same width, one address bus
			else if (type.equals("register file"))
			{
				if (getDataInputBlock().length!=1)
				{
					errorlog.add(new ErrorEntry(number,"must have one and only one data input"));
					return false;
				}
				if (getDataInputBlock()[0].bits!=bits)
				{
					errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
					return false;
				}
				if (name.equals("")) 
				{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
				}
				if (getAddressInputBlock().length!=1) 
				{
					errorlog.add(new ErrorEntry(number,"needs an address input"));
					return false;
				}
				return true;
			}
			//zero/one input bus, input bus has same width, one address bus
			else if (type.equals("memory")||type.equals("ports")||type.equals("lookup table"))
			{
				if (getDataInputBlock().length>1)
				{
					errorlog.add(new ErrorEntry(number,"can't have more than one data input"));
					return false;
				}
				if (getDataInputBlock().length>0 && getDataInputBlock()[0].bits!=bits)
				{
					errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
					return false;
				}
				if (getAddressInputBlock().length!=1) 
				{
					errorlog.add(new ErrorEntry(number,"needs an address input"));
					return false;
				}
				return true;
			}
			//one/two input bus, input bus has same width, one output bus, bus has same width
			else if (type.equals("ALU"))
			{
				if (getDataInputBlock().length<1 || getDataInputBlock().length>2 || getDataInputBlock()[0].bits!=bits || (getDataInputBlock().length>1 && getDataInputBlock()[1].bits!=bits))
				{
					errorlog.add(new ErrorEntry(number,"needs one or two input buses with the same number of bits as the ALU"));
					return false;
				}
				if (name.equals("")) 
				{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
				}
				return true;
			}
			//two input bus, same width, one output bus
			else if (type.equals("adder"))
			{
				if (getDataInputBlock().length!=2 || getDataInputBlock()[0].bits!=bits || getDataInputBlock()[1].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"needs two input buses with the same number of bits as the ALU"));
					return false;
					}
				return true;
			}
			//one or more input bus, same width
			else if (type.equals("combinational-adder")||type.equals("combinational-and")||type.equals("combinational-or")||type.equals("combinational-nand")||type.equals("combinational-nor")||type.equals("combinational-xor"))
			{
				if (getDataInputBlock().length<1) 
					{
					errorlog.add(new ErrorEntry(number,"needs an input bus"));
					return false;
					}
				for (int i=0; i<getDataInputBlock().length; i++) 
					if (getDataInputBlock()[i].bits!=bits)
						{
						errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
						return false;
						}
				return true;
			}
			//one input bus, same width
			else if (type.equals("combinational-not")||type.equals("combinational-negate")||type.equals("combinational-increment")||type.equals("combinational-decrement"))
			{
				if (getDataInputBlock().length!=1)
					{
					errorlog.add(new ErrorEntry(number,"must have one and only one input bus"));
					return false;
					}
				for (int i=0; i<getDataInputBlock().length; i++) 
					if (getDataInputBlock()[i].bits!=bits)
						{
						errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
						return false;
						}
				return true;
			}
			//two input buses, same width as each other, size=1
			else  if (type.equals("combinational-less-than")||type.equals("combinational-equal-to"))
			{
				if (getDataInputBlock().length!=2)
					{
					errorlog.add(new ErrorEntry(number,"must have exactly two input buses"));
					return false;
					}
				if (getDataInputBlock()[0].bits!=getDataInputBlock()[1].bits) 
					{
					errorlog.add(new ErrorEntry(number,"input buses must have the same number of bits"));
					return false;
					}
				if (bits!=1)
					{
					errorlog.add(new ErrorEntry(number,"can only have one bit"));
					return false;
					}
				return true;
			}
			//one or two input buses, same width
			else  if (type.equals("combinational-shift-right")||type.equals("combinational-shift-left"))
			{
				if (getDataInputBlock().length!=1 && getDataInputBlock().length!=2)
					{
					errorlog.add(new ErrorEntry(number,"must have one or two input buses"));
					return false;
					}
				for (int i=0; i<getDataInputBlock().length; i++) 
				{
					if (getDataInputBlock()[i].bits!=bits) 
						{
						errorlog.add(new ErrorEntry(number,"input buses must have the same width"));
						return false;
						}
				}
				return true;
			}
			//one output bus, one/more input bus, bus has valid width, no leftover bits
			else if (type.equals("joiner"))
			{
				if (getDataInputBlock().length<1) {
					errorlog.add(new ErrorEntry(number,"must have at least one input bus"));
					return false;
				}
				return true;
			}
			//one input bus, same width, one/more output bus, bus has valid width
			else if (type.equals("splitter"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have one input bus with the same number of bits"));
					return false;
					}
				return true;
			}
			//one input bus, same width, one/more output bus, bus has valid width, all outputs are one bit
			else if (type.equals("decoder"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have one input bus with the same number of bits"));
					return false;
					}
				for (int i=0; i<getDataOutputBlock().length; i++)
					if (getDataOutputBlock()[i].bits!=1)
					{
						errorlog.add(new ErrorEntry(number,"all output buses can only have one bit"));
						return false;
					}
				return true;
			}
			
			//one/more input bus, same width
			else if (type.equals("multiplexor"))
			{
				if (getDataInputBlock().length<1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have at least one input bus with the same number of bits"));
					return false;
					}
				if (name.equals("")) 
					{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
					}
				return true;
			}
			//one/more input bus, same width, one address bus
			else if (type.equals("data_multiplexor"))
			{
				if (getDataInputBlock().length<1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have at least one data input with the same number of bits"));
					return false;
					}
				if (getAddressInputBlock()==null) 
					{
					errorlog.add(new ErrorEntry(number,"must have a selector input"));
					return false;
					}
				return true;
			}
			//one output bus, bus has valid width
			else if (type.equals("constant"))
			{
				if (getDataInputBlock().length!=0) 
					{
					errorlog.add(new ErrorEntry(number,"constants can't have inputs"));
					return false;
					}
				if (name.equals("")||name.matches("[^0-9a-fA-F]+"))
					{
					errorlog.add(new ErrorEntry(number,"name must be a valid hexadecimal value"));
					return false;
					}
				return true;
			}
			else if (type.equals("extender"))
			{
				if (getDataInputBlock().length!=1) 
				{
				errorlog.add(new ErrorEntry(number,"must have one input"));
				return false;
				}
				return true;
			}
			//one input bus, same width
			else if (type.equals("control"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have one input with the same number of bits"));
					return false;
					}
				if (name.equals(""))
				{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
				}
				return true;
			}
			//one input bus, same width
			else if (type.equals("output pin"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"needs one input bus with the same number of bits"));
					return false;
					}
				return true;				
			}
			else if (type.equals("input pin"))
				return true;
			else if (type.equals("label"))
				return true;
			else
			{
				errorlog.add(new ErrorEntry(number,"isn't a recognized component"));
				return false;
			}
		}
	}
	public void move()
	{
		setStatusLabel("Drag to move pieces");
		action="move";
	}
	
	//duplicate all the selected pieces
	public void duplicate(int newx, int newy)
	{
		//save undo info
		undolog.push(dumpXML());

		ArrayList<Block> blockstoadd=new ArrayList<Block>();
		ArrayList<Bus> busestoadd=new ArrayList<Bus>();
		int basenumber=defaultModule.blocknumber;

		for (Bus b:defaultModule.buses)
		{
			if (b.selected)
			{
				Bus b2=new Bus(b);
				b2.number=b.number+basenumber;
				if (defaultModule.getPart(b2.input)!=null)
				{
					if (!defaultModule.getPart(b2.input).selected)
						b2.input=0;
					else
						b2.input+=basenumber;
				}
				if (defaultModule.getPart(b2.output)!=null)
				{
					if (!defaultModule.getPart(b2.output).selected)
						b2.output=0;
					else
						b2.output+=basenumber;
				}				
				busestoadd.add(b2);
			}
		}
		for (Block b:defaultModule.blocks)
		{
			if (b.selected)
			{
				Block b2;
				b2=new Block(b);
				b2.number=b.number+basenumber;
				if (b2.bus!=null)
				{
					for (Enumeration<Integer> e=b.bus.keys(); e.hasMoreElements();)
					{
						Integer i=e.nextElement();
						if (defaultModule.getBus(i.intValue()).selected)
							b2.bus.put(new Integer(basenumber+i.intValue()),b2.bus.get(i));
						b2.bus.remove(i);
					}
				}
				blockstoadd.add(b2);
			}
		}
		//find the top left
		int xcoor=blockstoadd.size()==0? busestoadd.size()==0? 0:busestoadd.get(0).xcoor : blockstoadd.get(0).xcoor;
		int ycoor=blockstoadd.size()==0? busestoadd.size()==0? 0:busestoadd.get(0).ycoor : blockstoadd.get(0).ycoor;
		for (Block b: blockstoadd)
		{
			if (b.xcoor<xcoor) xcoor=b.xcoor;
			if (b.ycoor<ycoor) ycoor=b.ycoor;
		}
		for (Bus b: busestoadd)
		{
			if (b.xcoor<xcoor) xcoor=b.xcoor;
			if (b.ycoor<ycoor) ycoor=b.ycoor;
		}
		int xshift=newx-xcoor; int yshift=newy-ycoor;
		for (Block b:blockstoadd)
		{
			b.xcoor+=xshift; b.xcoor2+=xshift; b.ycoor+=yshift; b.ycoor2+=yshift;
		}
		for (Bus b:busestoadd)
		{
			b.xcoor+=xshift; b.xcoor2+=xshift; b.ycoor+=yshift; b.ycoor2+=yshift;
		}		
		for (Block b:blockstoadd)
			defaultModule.blocks.add(b);
		for (Bus b:busestoadd)
			defaultModule.buses.add(b);
		defaultModule.blocknumber+=(blockstoadd.size()+busestoadd.size());
		postInvalidate();
	}

	
	public void undo()
	{
		if (undolog.size()==0)
			return;
		String newstate=undolog.pop();
		clearAll();
		
		int undosize=undolog.size();
		
		DatapathXMLParse xmlParse=new DatapathXMLParse(newstate,defaultModule);
		for (int i=1; i<=xmlParse.highestBlockNumber(); i++)
			xmlParse.constructBlock(i);
		
		int undosize2=undolog.size();
		for (int i=0; i<undosize2-undosize; i++)
			undolog.pop();
	}
	
	//read in a datapath xml file and break it down
	public class DatapathXMLParse
	{
		//list of tokens and contents
		String[] xmlParts;
		//the module into which the new blocks will be placed
		DatapathModule module;
		
		public DatapathXMLParse(String xml, DatapathModule module)
		{
			this.module=module;
			ArrayList<String> parts=new ArrayList<String>();
			int c=0;
			String tag="";

			//first break up everything by <>
			for (c=0; c<xml.length(); c++)
			{
				if (xml.charAt(c)=='<')
				{
					if (!isWhiteSpace(tag))
						parts.add(tag);
					tag="<";
				}
				else if (xml.charAt(c)=='>')
				{
					tag+=">";
					parts.add(tag);
					tag="";
				}
				else
					tag+=xml.charAt(c);
			}

			xmlParts=new String[parts.size()];
			for (int i=0; i<parts.size(); i++)
				xmlParts[i]=(String)parts.get(i);

//			for (int i=0; i<parts.size(); i++)
//				System.out.println(xmlParts[i]);
		}
		
		//find the next instance of token in the list
		public int find(String token, int starting)
		{
			for (int i=starting; i<xmlParts.length; i++)
			{
				if (xmlParts[i].equals(token))
						return i;
			}
			return -1;
		}

		//true is s only contains whitespace
		private boolean isWhiteSpace(String s)
		{
			for (int i=0; i<s.length(); i++)
			{
				if (s.charAt(i)!=' '&&s.charAt(i)!='\t'&&s.charAt(i)!='\n')
					return false;
			}
			return true;
		}

		//find where block "number" occurs in the xml, and extract all of its fields into a big array
		public String[] extractBlock(int number)
		{
			int i,j;
			for(i=0; i<xmlParts.length; i++)
			{
				if (xmlParts[i].equals("<number>") && Integer.parseInt(xmlParts[i+1])==number)
					break;
			}
			if (i==xmlParts.length)
				return null;
			for (j=i-1; ; j++)
			{
				if (xmlParts[j].equals("</"+xmlParts[i-1].substring(1,xmlParts[i-1].length())))
					break;
			}
			String[] block=new String[j-i+2];
			for (int k=i-1; k<=j; k++)
				block[k-(i-1)]=xmlParts[k];
			return block;
		}

		//given a token, return its contents
		public String extractField(String[] block, String field)
		{
			for (int i=0; i<block.length; i++)
			{
				if (block[i].equals(field))
					return block[i+1];
			}
			return null;
		}

		//return the number of the highest-numbered block
		public int highestBlockNumber()
		{
			int number=0;
			for(int i=0; i<xmlParts.length; i++)
			{
				if (xmlParts[i].equals("<number>") && Integer.parseInt(xmlParts[i+1])>number)
					number=Integer.parseInt(xmlParts[i+1]);
			}
			return number;
		}

		//given a block number, construct it and place it in the module
		public void constructBlock(int number)
		{
			//first get its parts
			String[] block=extractBlock(number);
			if (block==null) return;
			
			//get the bits field
			int bits=0;
			if (extractField(block,"<bits>")!=null)
				bits=Integer.parseInt(extractField(block,"<bits>"));
			String type=block[0].substring(1,block[0].length()-1);

			//two possibilities: it's a block or a bus
			Block b=null;
			Bus bu=null;
			if (type.equals("bus"))
				bu=new Bus(bits,module);
			else
				b=new Block(type,bits,module);
			//get the location coordinates, entry and exit buses
			int x=Integer.parseInt(extractField(block,"<xcoordinate>"));
			int y=Integer.parseInt(extractField(block,"<ycoordinate>"));
			int x2=0,y2=0,entry=0,exit=0;
			if (extractField(block,"<xcoordinate2>")!=null)
				x2=Integer.parseInt(extractField(block,"<xcoordinate2>"));
			if (extractField(block,"<ycoordinate2>")!=null)
				y2=Integer.parseInt(extractField(block,"<ycoordinate2>"));
			if (extractField(block,"<description>")!=null && !extractField(block,"<description>").equals("</description>"))
			{
				if (b==null)
					bu.description=extractField(block,"<description>");
				else
					b.description=extractField(block,"<description>");
			}
			if (extractField(block,"<entry>")!=null)
			{
				entry=Integer.parseInt(extractField(block,"<entry>"));
				if (entry!=0) entry+=module.basenumber;
			}
			if (extractField(block,"<exit>")!=null)
			{
				exit=Integer.parseInt(extractField(block,"<exit>"));
				if (exit!=0) exit+=module.basenumber;
			}
			//we now can create it
			if (type.equals("bus"))
			{
				bu.place(x,y,x2,y2,entry,exit);
				module.addBlock(bu);
			}
			else
			{
				b.place(x,y,x2,y2);
				module.addBlock(b);
			}
			//if it's a splitter, link on the output buses
			if (type.equals("splitter"))
			{
				for (int i=0; i<block.length; i++)
				{
					if (block[i].length()>6 && block[i].substring(0,6).equals("<line "))
					{
						int j=Integer.parseInt(block[i].substring(6,block[i].length()-1));
						if (j!=0)
							j+=module.basenumber;
						b.bus.put(new Integer(j),block[i+1]);
					}
				}
			}
			//if it's a lookup table, populate its entries
			if (type.equals("lookup table"))
			{
				for (int i=0; i<block.length; i++)
				{
					if (block[i].length()>7 && block[i].substring(0,7).equals("<value "))
					{
						int j=Integer.parseInt(block[i].substring(7,block[i].length()-1));
						b.regfilevalue.put(new Integer(j),Long.parseLong(block[i+1],16));
					}
				}				
			}
			//give the block a number and a name
			if (b!=null)
			{
				b.number=module.basenumber+number;
				module.blocknumber=b.number+1;
				b.name=extractField(block,"<name>");
				if (b.name.equals("</name>"))
					b.name="";
			}
			else
			{
				bu.number=module.basenumber+number;
				module.blocknumber=bu.number+1;
				bu.name=extractField(block,"<name>");
				if (bu.name.equals("</name>"))
					bu.name="";				
			}
		}
	}
	public abstract class Part
	{
		public String name="",description="";
		public int xcoor,ycoor,xcoor2,ycoor2;
		public int number;
		public boolean selected=false;
		public boolean highlighted=false;
		public long value=0;		
		public abstract Part[] getInputBlocks();
		public String type;
		public int bits;
		public abstract void setValue(long value);
		public abstract long getValue();
	}
	public class DatapathModule
	{
		public ArrayList<Block> blocks;
		public ArrayList<Bus> buses;	
		public int blocknumber=1;
//		private Stack<String> undolog;
		public int basenumber=0;
		
		public DatapathModule()
		{
			blocks=new ArrayList<Block>();
			buses=new ArrayList<Bus>();
			blocknumber=1;			
		}
		
		public Block createBlock(String name, int bits)
		{
			return new Block(name,bits,this);
		}
		public Bus createBus(Block b)
		{
			return new Bus(b, this);
		}
		public Bus createBus(Bus b)
		{
			return new Bus(b, this);
		}
		
		public void addBlock(Block b)
		{
			if (this==defaultModule) undolog.push(dumpXML());
			b.number=blocknumber++;
			if (b.type.length()>14 && b.type.substring(0,14).equals("combinational-"))
				b.name=b.type.substring(14)+b.number;
			else
				b.name=b.type+b.number;
			if (b.type.equals("constant"))
				b.name="0";
			blocks.add(b);
		}
		public void addBlock(Bus b)
		{
			if (this==defaultModule) undolog.push(dumpXML());
			b.number=blocknumber++;
			buses.add(b);
			if (b.output!=0 && getBlock(b.output)!=null && getBlock(b.output).type.equals("joiner"))
				getBlock(b.output).updateJoinerBits();
		}
		public Block getBlock(int number)
		{
			if (number<=0) return null;
			for (Block b:blocks)
				if (b.number==number) return b;
			return null;
		}
		public Block getBlock(String name)
		{
			for (Block b:blocks)
				if (b.name.equals(name)) return b;
			return null;
		}
		public Bus getBus(int number)
		{
			if (number<=0) return null;
			for (Bus b:buses)
				if (b.number==number) return b;
			return null;
		}
		public Part getPart(int number)
		{
			if (number<=0) return null;
			for (Block b:blocks)
				if (b.number==number) return b;
			for (Bus b:buses)
				if (b.number==number) return b;
			return null;		
		}

		public void resetAll()
		{
			for (Block b:blocks)
				if (!b.type.equals("ports")&&!b.type.equals("memory"))
					b.setValue(0);
			for (Bus b:buses)
				b.setValue(0);
		}
/*		public void clockAll()
		{
			for (Block b:blocks)
				b.doClock();
		}*/
		public void resetClocks()
		{
			for (Block b:blocks)
				b.resetClock();
		}	
		public void resetHighlights()
		{
			for (Block b:blocks)
				b.highlighted=false;
			for (Bus b:buses)
				b.highlighted=false;
		}	
		public void propagateAll()
		{
			for (int i=0; i<blocks.size()+buses.size(); i++)
			{
			for (Block b:blocks)
				b.doPropagate();
			for (Bus b:buses)
				b.doPropagate();
			}
		}
		public void fixbuses()
		{
			for (int i=0; i<buses.size(); i++)
				for (Bus b:buses)
					b.fix();
		}
		//find the shortest sequence of wires and muxes leading from one block to another
		public Part[] tracePath(String inputBlockName, String outputBlockName)
		{
			Block inputBlock = getBlock(inputBlockName);
			Block outputBlock = getBlock(outputBlockName);
			return tracePath(inputBlock,outputBlock);
		}
		public Part[] tracePath(Block inputBlock, Block outputBlock)
		{
			//queue for the BFS
			ArrayList<Part> searchq=new ArrayList<Part>();
			//all blocks sourced by inputBlock
			ArrayList<Part> connectedq=new ArrayList<Part>();
			//for each sourced block, who sources it -- defines a tree
			ArrayList<Part> sourceq=new ArrayList<Part>();
			searchq.add(inputBlock);
			boolean foundit=false;
			while(!foundit)
			{
				//check if we couldn't find a connection
				if (searchq.size()==0)
					break;
				Part currentBlock=searchq.remove(0);
				for (Block b:blocks)
				{
					//if it's not a single-input stateless block, it can't be along the trace path
					if (b!=outputBlock && !b.type.equals("multiplexor")&&!b.type.equals("extender")&&!b.type.equals("splitter"))
						continue;
					//go through the block's inputs, see if currentBlock is among them
					Part[] outputsInputs=b.getInputBlocks();
					boolean ontree=false;
					for (int i=0; i<outputsInputs.length; i++)
						if (outputsInputs[i]==currentBlock)
							ontree=true;
					if (!ontree) continue;
					searchq.add(b);
					connectedq.add(b);
					sourceq.add(currentBlock);
					if (b==outputBlock)
					{
						foundit=true;
						break;
					}
				}
				for (Bus b:buses)
				{
					//go through the block's inputs, see if currentBlock is among them
					Part[] outputsInputs=b.getInputBlocks();
					boolean ontree=false;
					for (int i=0; i<outputsInputs.length; i++)
						if (outputsInputs[i]==currentBlock)
							ontree=true;
					if (!ontree) continue;
					searchq.add(b);
					connectedq.add(b);
					sourceq.add(currentBlock);
				}
			}
			//there is no path? quit
			if (!foundit)
				return null;
			//now let's construct our path
			ArrayList<Part> pathq=new ArrayList<Part>();
			//start at the end
			Part currentBlock=outputBlock;
			while(currentBlock!=inputBlock)
			{
				pathq.add(currentBlock);
				currentBlock=sourceq.get(connectedq.indexOf(currentBlock));
			}
			//add the input block on
			pathq.add(currentBlock);
			Part[] ret=new Part[pathq.size()];
			for (int i=0; i<pathq.size(); i++)
				ret[i]=pathq.get(i);
			return ret;
		}
		public String[] controlOutputs()
		{
			int i=0;
			for (Block b:blocks)
			{
				if (!b.controlOutputs().equals(""))
					i++;
			}
			String[] c=new String[i];
			i=0;
			for (Block b:blocks)
			{
				if (!b.controlOutputs().equals(""))
				{
					c[i++]=b.controlOutputs();
				}
			}
			return c;
		}

		public String[] controlInputs()
		{
			int i=0;
			for (Block b:blocks)
			{
				if (!b.controlInputs().equals(""))
					i++;
			}
			String[] c=new String[i];
			i=0;
			for (Block b:blocks)
			{
				if (!b.controlInputs().equals(""))
				{
					c[i++]=b.controlInputs();
				}
			}
			return c;
		}
		public String dumpXML()
		{
			String xml="<processor>\n\n";
			for (Block b:blocks)
			{
				xml+=b.getXML()+"\n";
			}
			for (Bus b:buses)
			{
				xml+=b.getXML()+"\n";
			}
			xml+="</processor>\n";
			return xml;
		}
	}
	public class CustomProcessorModule
	{
		public boolean active=false;
		public boolean updateGUIs=false;
		public DatapathModule datapath;
		public CustomProcessorModule(DatapathModule datapath)
		{
			this.datapath=datapath;
			initialize();
		}
		public void initialize()
		{
			datapath.propagateAll();
			doAllPaths();
			datapath.propagateAll();			
		}
		public void doCycle()
		{
			if (!active) return;
			datapath.resetHighlights();
			datapath.propagateAll();
			doAllPaths();
			datapath.propagateAll();
			if (updateGUIs)
			{
				postInvalidate();
			}
		}

		public void doAllPaths()
		{
			datapath.resetClocks();
			for (Block b:defaultModule.blocks)
			{
				b.clockSetting=true;
				b.doClock();
			}
		}
	}

	
	private void editBlock(Part block)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(logicmaker);
		builder.setTitle(block.type+" "+block.name);
		builder.setCancelable(true);

		final ModificationView modview=new ModificationView(block);
		builder.setView(modview.layout);
		builder.setPositiveButton("Modify", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				modview.doModify();
				}
			});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				}
			});
		AlertDialog box = builder.create();
		box.show();	
	}
	
	private class ModificationView
	{
		EditText namefield,bitfield,indexfield,lowbitfield,highbitfield,valuefield;
		Button lookup;
		LinearLayout layout;
		Part block;
		
		public ModificationView(final Part block)
		{
			super();
			
			this.block=block;
			int type=0;
			if (block.getInputBlocks().length>0 && block.getInputBlocks()[0]!=null && block.getInputBlocks()[0].type.equals("splitter"))
				type=1;
			if(block.type.equals("register file") || block.type.equals("memory") || block.type.equals("ports") || block.type.equals("lookup table"))
				type=2;
			
			Context c=logicmaker;
			layout = new LinearLayout(c);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setBackgroundColor(Color.WHITE);
			TextView text;
			text=new TextView(c);
			text.setText("Name:");
			layout.addView(text);
			namefield=new EditText(c);
			namefield.setText(block.name);
			layout.addView(namefield);
			text=new TextView(c);
			text.setText("Bits:");
			layout.addView(text);
			bitfield=new EditText(c);
			bitfield.setText(""+block.bits);
			layout.addView(bitfield);
			if (type==1)
			{
				LinearLayout hlayout=new LinearLayout(c);
				hlayout.setOrientation(LinearLayout.HORIZONTAL);
				text=new TextView(c);
				text.setText("High Bit:");
				hlayout.addView(text);
				highbitfield=new EditText(c);
				highbitfield.setText("");
				hlayout.addView(highbitfield);
				text=new TextView(c);
				text.setText("Low Bit:");
				hlayout.addView(text);
				lowbitfield=new EditText(c);
				lowbitfield.setText("");
				hlayout.addView(lowbitfield);
				layout.addView(hlayout);
				Block splitter=(Block)block.getInputBlocks()[0];
				highbitfield.setText(""+(block.bits-1));
				lowbitfield.setText("0");
				for (Enumeration e=splitter.bus.keys(); e.hasMoreElements();)
				{
					Integer splitterkey=(Integer)(e.nextElement());
					int i=splitterkey.intValue();

					if (block.number==i)
					{
						String busstring=(String)splitter.bus.get(splitterkey);
						int b1=Integer.parseInt(busstring.substring(0,busstring.indexOf(":")));
						int b2=Integer.parseInt(busstring.substring(busstring.indexOf(":")+1,busstring.length()));
						highbitfield.setText(""+b1);
						lowbitfield.setText(""+b2);
						break;
					}
				}
			}
			if (type==2)
			{
				LinearLayout hlayout=new LinearLayout(c);
				hlayout.setOrientation(LinearLayout.HORIZONTAL);
				text=new TextView(c);
				text.setText("Index:");
				hlayout.addView(text);
				indexfield=new EditText(c);
				indexfield.setText("0");
				hlayout.addView(indexfield);
				text=new TextView(c);
				text.setText("Value:");
				hlayout.addView(text);
				valuefield=new EditText(c);
				valuefield.setText(""+Long.toHexString(((Block)block).getValue(Integer.parseInt(indexfield.getText().toString(),16))));						
				hlayout.addView(valuefield);
				lookup=new Button(c);
				lookup.setText("Lookup");
				hlayout.addView(lookup);
				lookup.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) 
					{
						valuefield.setText(""+Long.toHexString(((Block)block).getValue(Integer.parseInt(indexfield.getText().toString(),16))));						
					}
				});
				layout.addView(hlayout);			
			}
			if (isSimulating && valuefield==null)
			{
				text=new TextView(c);
				text.setText("Value:");
				layout.addView(text);
				valuefield=new EditText(c);
				valuefield.setText(Long.toHexString(block.value));
				layout.addView(valuefield);
			}			
		}
		public void doModify()
		{
			block.name=namefield.getText().toString();
			block.bits=Integer.parseInt(bitfield.getText().toString());
			if (valuefield!=null)
			{
				if(block.type.equals("register file") || block.type.equals("memory") || block.type.equals("ports") || block.type.equals("lookup table"))
					((Block)block).setValue(Integer.parseInt(indexfield.getText().toString(),16),Long.parseLong(valuefield.getText().toString(),16));
				else
					block.setValue(Long.parseLong(valuefield.getText().toString(),16));								
				propagateAll();
			}
			if (highbitfield!=null)
			{
				Block splitter=(Block)block.getInputBlocks()[0];
				for (Enumeration e=splitter.bus.keys(); e.hasMoreElements();)
				{
					Integer splitterkey=(Integer)(e.nextElement());
					int i=splitterkey.intValue();

					if (block.number==i)
						splitter.bus.remove(splitterkey);
				}
				String busstring=highbitfield.getText().toString()+":"+lowbitfield.getText().toString();
				splitter.bus.put(new Integer(block.number), busstring);
				block.bits=Integer.parseInt(highbitfield.getText().toString())-Integer.parseInt(lowbitfield.getText().toString())+1;
				bitfield.setText(""+block.bits);							
			}
			if (valuefield!=null)
			{
				block.setValue(Long.parseLong(valuefield.getText().toString(),16));
				propagateAll();
			}
		
			defaultbits=Integer.parseInt(bitfield.getText().toString());
			postInvalidate();
		}
	}
}
