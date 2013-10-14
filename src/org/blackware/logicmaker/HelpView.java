package org.blackware.logicmaker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class HelpView extends View 
{
	static String infoString="LogicMaker is an educational digital circuit design tool for Android smartphones and tablets.  You can use it to build logic circuits out of gates, registers, adders, and other building blocks.  With LogicMaker, you can build simple combinational circuits and entire CPU datapaths.";
	static String aboutString="LogicMaker was created by Michael Black as part of the EmuMaker 86 project, 2010-2013.  It is freely distributed under the GNU Public License.  \nFor the source, go to http://emumaker86.org";
	static String[] howDoIString={
		"Get started","Start by trying out the example circuits:\n - Choose an example from the help screen.\n- Press Simulate to start the example.\n- Tap the input pins to change the input values.\n- Tap the screen to clock the registers.",
		"Place components","- Select a component from the dropdown list of blocks.\n- Place a block by tapping on the screen.\n - Use bus to drag wires between blocks.\n",
		"Place a bus","- First place the blocks you want to connect.\n- Tap the Bus button\n- Press on the bottom of the source block and start dragging.\n- Release at the top of the destination block (it should change color)\n- Depending on your path you may have to draw several bus links to connect two blocks.",
		"Connect a bus to a block","- Buses should connect to top of a block to source it.\n- If the bus addresses a block, such as a multiplexor or lookup table, it should go to the side.\n- A bus connects if the block turns color when you connect to it.",
		"Alter a block","- Press and hold on the block.\n- A menu will pop up\n -Press update to save your changes",
		"Select a block","- Tap a block to select it.\n- Tap again to unselect it.\n Long press the background to unselect everybody",
		"Fix a mistake","Two ways:\n- Press the back button to undo\n- Select the block and press Delete",
		"Simulate","Press the Simulate button when your circuit is ready.\nIf there are errors, it will not simulate and the bad blocks will be highlighted.\n  Press simulate again to stop the simulation",
		"Handle common errors","- Most blocks, like registers and gates need an input bus.  Input pins are the exception.\n- Buses must have the same number of bits as the blocks they connect to.\n- Lookup tables and register files need an address bus.\n- If you delete a block, you must delete the buses it sourced.\n- Some bad buses may be tiny or hard to see.  Press Unselect, Verify, and Delete to get rid of them.",
	};
	static String[] blockString={
		"Input pin","Input pins are for making user input to your circuit.  An input pin provides a value to a bus connecting to the bottom of the pin. You can set the number of bits the pin holds.  The value of input pins can be altered while the datapath is running by either clicking them, incrementing the value, or by long-pressing them and changing the “Value” field.",
		"Output pin","Output pins display a value while the datapath is running. The value is provided through a bus into the top of the pin.",
		"Bus","A bus is a set of wires connecting the output of one block to the input of another. Press Bus, touch a source block, and drag to draw a bus. Long-press on the bus to modify it. If the bus is sourced by a splitter, you can also set the range of bits it carries.",
		"Register","A register is a storage unit that saves the value from its input bus on each clock cycle (a clock cycle is a tap on the screen when the simulation is running). You can control whether the register is enabled or disabled (whether or not it will save the new value) by connecting a 1-bit bus to the left-hand side, which will disable the register when it is 0. Input to the register should go into the top, and output is carried out through a bus at the bottom. Both input and output buses must handle the same number of bits as the register.",
		"Flag","A flag is a one bit register",
		"Register File","A register file is a table of registers in one block. Control which register is saving the input value using a bus connected to the left-hand side of the block.",
		"Multiplexor","A multiplexor allows you to route multiple input buss to a single output bus. The input buses are numbered starting from 0 at the farthest left. Select an input using a bus connected to the side.",
		"Constant","A constant provides an unchanging hexidecimal value through an output bus connected to the bottom of the block. The bus must have enough bits to carry the value of the constant. Set the value of the constant by putting it as the name of the block.",
		"Lookup table","A lookup table is a ROM that holds a truth table. Set the values in the table by long-pressing on it to open the options window. Then, set the index to the first address, and Value to the corresponding value, and click Update. Continue inputing lines to the table one at a time, making sure to click Update in between each line. To access the memory, connect a bus to the side, which will provide the desired index, and an output bus to the bottom of the block to carry the value.",
		"AND, OR, NAND...","Basic logic gates.  They should have one output bus at the bottom and two or more inputs buses at the top.",
		"Adder, Equal to ...","Logic blocks to do simple combinational functions.  They should have one output bus at the bottom and two input buses at the top.",
		"Splitter","Allows you to extract wires from a bus.  Connect the source to the top of the splitter.  From the bottom, draw a bus.  Then long-press on the output bus, and choose which pins it should carry.  Pins are numbered from the right starting at 0.",
		"Joiner","Puts buses together to make a bigger bus.  Buses are joined in order, with the right-most being the least significant bits.",
		"Label","Place a text field to comment your work.  Labels have no effect on the circuit.",
	};
	static String[] examples={
		"Simple NAND","<processor><combinational-nand><number>1</number><name>nand</name><bits>1</bits><xcoordinate>194</xcoordinate><ycoordinate>72</ycoordinate><xcoordinate2>234</xcoordinate2><ycoordinate2>87</ycoordinate2><description></description></combinational-nand><input pin><number>2</number><name>A</name><bits>1</bits><xcoordinate>176</xcoordinate><ycoordinate>34</ycoordinate><xcoordinate2>189</xcoordinate2><ycoordinate2>44</ycoordinate2><description></description></input pin><input pin><number>3</number><name>B</name><bits>1</bits><xcoordinate>248</xcoordinate><ycoordinate>36</ycoordinate><xcoordinate2>261</xcoordinate2><ycoordinate2>46</ycoordinate2><description></description></input pin><output pin><number>4</number><name>Y</name><bits>1</bits><xcoordinate>203</xcoordinate><ycoordinate>109</ycoordinate><xcoordinate2>216</xcoordinate2><ycoordinate2>119</ycoordinate2><description></description></output pin><bus><number>5</number><name></name><bits>1</bits><xcoordinate>182</xcoordinate><ycoordinate>44</ycoordinate><xcoordinate2>182</xcoordinate2><ycoordinate2>60</ycoordinate2><description></description><entry>2</entry><exit>0</exit></bus><bus><number>6</number><name></name><bits>1</bits><xcoordinate>182</xcoordinate><ycoordinate>60</ycoordinate><xcoordinate2>204</xcoordinate2><ycoordinate2>60</ycoordinate2><description></description><entry>5</entry><exit>0</exit></bus><bus><number>7</number><name></name><bits>1</bits><xcoordinate>200</xcoordinate><ycoordinate>60</ycoordinate><xcoordinate2>200</xcoordinate2><ycoordinate2>72</ycoordinate2><description></description><entry>6</entry><exit>1</exit></bus><bus><number>8</number><name></name><bits>1</bits><xcoordinate>254</xcoordinate><ycoordinate>46</ycoordinate><xcoordinate2>254</xcoordinate2><ycoordinate2>58</ycoordinate2><description></description><entry>3</entry><exit>0</exit></bus><bus><number>9</number><name></name><bits>1</bits><xcoordinate>254</xcoordinate><ycoordinate>58</ycoordinate><xcoordinate2>216</xcoordinate2><ycoordinate2>58</ycoordinate2><description></description><entry>8</entry><exit>0</exit></bus><bus><number>10</number><name></name><bits>1</bits><xcoordinate>224</xcoordinate><ycoordinate>58</ycoordinate><xcoordinate2>224</xcoordinate2><ycoordinate2>72</ycoordinate2><description></description><entry>9</entry><exit>1</exit></bus><bus><number>11</number><name></name><bits>1</bits><xcoordinate>214</xcoordinate><ycoordinate>87</ycoordinate><xcoordinate2>214</xcoordinate2><ycoordinate2>109</ycoordinate2><description></description><entry>1</entry><exit>4</exit></bus></processor>",
		"Counter","<processor><register><number>1</number><name>count</name><bits>8</bits><xcoordinate>212</xcoordinate><ycoordinate>98</ycoordinate><xcoordinate2>252</xcoordinate2><ycoordinate2>128</ycoordinate2><description></description></register><combinational-adder><number>2</number><name>adder2</name><bits>8</bits><xcoordinate>302</xcoordinate><ycoordinate>152</ycoordinate><xcoordinate2>342</xcoordinate2><ycoordinate2>167</ycoordinate2><description></description></combinational-adder><combinational-adder><number>3</number><name>adder3</name><bits>8</bits><xcoordinate>378</xcoordinate><ycoordinate>160</ycoordinate><xcoordinate2>418</xcoordinate2><ycoordinate2>175</ycoordinate2><description></description></combinational-adder><constant><number>11</number><name>1</name><bits>8</bits><xcoordinate>328</xcoordinate><ycoordinate>118</ycoordinate><xcoordinate2>348</xcoordinate2><ycoordinate2>133</ycoordinate2><description></description></constant><constant><number>12</number><name>-1</name><bits>8</bits><xcoordinate>404</xcoordinate><ycoordinate>124</ycoordinate><xcoordinate2>424</xcoordinate2><ycoordinate2>139</ycoordinate2><description></description></constant><multiplexor><number>15</number><name>multiplexor15</name><bits>8</bits><xcoordinate>343</xcoordinate><ycoordinate>188</ycoordinate><xcoordinate2>383</xcoordinate2><ycoordinate2>203</ycoordinate2><description></description></multiplexor><input pin><number>29</number><name>up/down</name><bits>1</bits><xcoordinate>274</xcoordinate><ycoordinate>102</ycoordinate><xcoordinate2>287</xcoordinate2><ycoordinate2>112</ycoordinate2><description></description></input pin><output pin><number>30</number><name>count</name><bits>8</bits><xcoordinate>252</xcoordinate><ycoordinate>156</ycoordinate><xcoordinate2>265</xcoordinate2><ycoordinate2>166</ycoordinate2><description></description></output pin><bus><number>4</number><name></name><bits>8</bits><xcoordinate>232</xcoordinate><ycoordinate>128</ycoordinate><xcoordinate2>232</xcoordinate2><ycoordinate2>138</ycoordinate2><description></description><entry>1</entry><exit>0</exit></bus><bus><number>5</number><name></name><bits>8</bits><xcoordinate>232</xcoordinate><ycoordinate>138</ycoordinate><xcoordinate2>390</xcoordinate2><ycoordinate2>138</ycoordinate2><description></description><entry>4</entry><exit>0</exit></bus><bus><number>6</number><name></name><bits>8</bits><xcoordinate>316</xcoordinate><ycoordinate>138</ycoordinate><xcoordinate2>316</xcoordinate2><ycoordinate2>152</ycoordinate2><description></description><entry>5</entry><exit>2</exit></bus><bus><number>10</number><name></name><bits>8</bits><xcoordinate>390</xcoordinate><ycoordinate>138</ycoordinate><xcoordinate2>390</xcoordinate2><ycoordinate2>160</ycoordinate2><description></description><entry>5</entry><exit>3</exit></bus><bus><number>13</number><name></name><bits>8</bits><xcoordinate>338</xcoordinate><ycoordinate>133</ycoordinate><xcoordinate2>338</xcoordinate2><ycoordinate2>152</ycoordinate2><description></description><entry>11</entry><exit>2</exit></bus><bus><number>14</number><name></name><bits>8</bits><xcoordinate>414</xcoordinate><ycoordinate>139</ycoordinate><xcoordinate2>414</xcoordinate2><ycoordinate2>160</ycoordinate2><description></description><entry>12</entry><exit>3</exit></bus><bus><number>16</number><name></name><bits>8</bits><xcoordinate>322</xcoordinate><ycoordinate>167</ycoordinate><xcoordinate2>322</xcoordinate2><ycoordinate2>168</ycoordinate2><description></description><entry>2</entry><exit>0</exit></bus><bus><number>17</number><name></name><bits>8</bits><xcoordinate>322</xcoordinate><ycoordinate>168</ycoordinate><xcoordinate2>358</xcoordinate2><ycoordinate2>168</ycoordinate2><description></description><entry>16</entry><exit>0</exit></bus><bus><number>18</number><name></name><bits>8</bits><xcoordinate>358</xcoordinate><ycoordinate>168</ycoordinate><xcoordinate2>358</xcoordinate2><ycoordinate2>188</ycoordinate2><description></description><entry>17</entry><exit>15</exit></bus><bus><number>19</number><name></name><bits>8</bits><xcoordinate>398</xcoordinate><ycoordinate>175</ycoordinate><xcoordinate2>398</xcoordinate2><ycoordinate2>182</ycoordinate2><description></description><entry>3</entry><exit>0</exit></bus><bus><number>20</number><name></name><bits>8</bits><xcoordinate>398</xcoordinate><ycoordinate>182</ycoordinate><xcoordinate2>372</xcoordinate2><ycoordinate2>182</ycoordinate2><description></description><entry>19</entry><exit>0</exit></bus><bus><number>21</number><name></name><bits>8</bits><xcoordinate>380</xcoordinate><ycoordinate>182</ycoordinate><xcoordinate2>380</xcoordinate2><ycoordinate2>188</ycoordinate2><description></description><entry>20</entry><exit>15</exit></bus><bus><number>22</number><name></name><bits>8</bits><xcoordinate>363</xcoordinate><ycoordinate>203</ycoordinate><xcoordinate2>363</xcoordinate2><ycoordinate2>208</ycoordinate2><description></description><entry>15</entry><exit>0</exit></bus><bus><number>23</number><name></name><bits>8</bits><xcoordinate>363</xcoordinate><ycoordinate>208</ycoordinate><xcoordinate2>208</xcoordinate2><ycoordinate2>208</ycoordinate2><description></description><entry>22</entry><exit>0</exit></bus><bus><number>24</number><name></name><bits>8</bits><xcoordinate>216</xcoordinate><ycoordinate>208</ycoordinate><xcoordinate2>216</xcoordinate2><ycoordinate2>144</ycoordinate2><description></description><entry>23</entry><exit>0</exit></bus><bus><number>25</number><name></name><bits>8</bits><xcoordinate>216</xcoordinate><ycoordinate>144</ycoordinate><xcoordinate2>186</xcoordinate2><ycoordinate2>144</ycoordinate2><description></description><entry>24</entry><exit>0</exit></bus><bus><number>26</number><name></name><bits>8</bits><xcoordinate>194</xcoordinate><ycoordinate>144</ycoordinate><xcoordinate2>194</xcoordinate2><ycoordinate2>86</ycoordinate2><description></description><entry>25</entry><exit>0</exit></bus><bus><number>27</number><name></name><bits>8</bits><xcoordinate>194</xcoordinate><ycoordinate>86</ycoordinate><xcoordinate2>240</xcoordinate2><ycoordinate2>86</ycoordinate2><description></description><entry>26</entry><exit>0</exit></bus><bus><number>28</number><name></name><bits>8</bits><xcoordinate>240</xcoordinate><ycoordinate>86</ycoordinate><xcoordinate2>240</xcoordinate2><ycoordinate2>98</ycoordinate2><description></description><entry>27</entry><exit>1</exit></bus><bus><number>31</number><name></name><bits>8</bits><xcoordinate>260</xcoordinate><ycoordinate>138</ycoordinate><xcoordinate2>260</xcoordinate2><ycoordinate2>156</ycoordinate2><description></description><entry>5</entry><exit>30</exit></bus><bus><number>32</number><name></name><bits>1</bits><xcoordinate>280</xcoordinate><ycoordinate>112</ycoordinate><xcoordinate2>280</xcoordinate2><ycoordinate2>200</ycoordinate2><description></description><entry>29</entry><exit>0</exit></bus><bus><number>33</number><name></name><bits>1</bits><xcoordinate>280</xcoordinate><ycoordinate>200</ycoordinate><xcoordinate2>343</xcoordinate2><ycoordinate2>200</ycoordinate2><description></description><entry>32</entry><exit>15</exit></bus></processor>",
		"RS Flip Flop","<processor><combinational-nor><number>1</number><name>nor1</name><bits>1</bits><xcoordinate>130</xcoordinate><ycoordinate>72</ycoordinate><xcoordinate2>170</xcoordinate2><ycoordinate2>87</ycoordinate2><description></description></combinational-nor><combinational-nor><number>2</number><name>nor2</name><bits>1</bits><xcoordinate>222</xcoordinate><ycoordinate>76</ycoordinate><xcoordinate2>262</xcoordinate2><ycoordinate2>91</ycoordinate2><description></description></combinational-nor><input pin><number>3</number><name>reset</name><bits>1</bits><xcoordinate>134</xcoordinate><ycoordinate>34</ycoordinate><xcoordinate2>147</xcoordinate2><ycoordinate2>44</ycoordinate2><description></description></input pin><input pin><number>4</number><name>set</name><bits>1</bits><xcoordinate>250</xcoordinate><ycoordinate>42</ycoordinate><xcoordinate2>263</xcoordinate2><ycoordinate2>52</ycoordinate2><description></description></input pin><output pin><number>5</number><name>output</name><bits>1</bits><xcoordinate>142</xcoordinate><ycoordinate>132</ycoordinate><xcoordinate2>155</xcoordinate2><ycoordinate2>142</ycoordinate2><description></description></output pin><bus><number>6</number><name></name><bits>1</bits><xcoordinate>150</xcoordinate><ycoordinate>87</ycoordinate><xcoordinate2>150</xcoordinate2><ycoordinate2>132</ycoordinate2><description></description><entry>1</entry><exit>5</exit></bus><bus><number>7</number><name></name><bits>1</bits><xcoordinate>150</xcoordinate><ycoordinate>112</ycoordinate><xcoordinate2>206</xcoordinate2><ycoordinate2>112</ycoordinate2><description></description><entry>6</entry><exit>0</exit></bus><bus><number>8</number><name></name><bits>1</bits><xcoordinate>206</xcoordinate><ycoordinate>112</ycoordinate><xcoordinate2>206</xcoordinate2><ycoordinate2>60</ycoordinate2><description></description><entry>7</entry><exit>0</exit></bus><bus><number>9</number><name></name><bits>1</bits><xcoordinate>206</xcoordinate><ycoordinate>62</ycoordinate><xcoordinate2>240</xcoordinate2><ycoordinate2>62</ycoordinate2><description></description><entry>8</entry><exit>0</exit></bus><bus><number>10</number><name></name><bits>1</bits><xcoordinate>240</xcoordinate><ycoordinate>62</ycoordinate><xcoordinate2>240</xcoordinate2><ycoordinate2>76</ycoordinate2><description></description><entry>9</entry><exit>2</exit></bus><bus><number>11</number><name></name><bits>1</bits><xcoordinate>256</xcoordinate><ycoordinate>52</ycoordinate><xcoordinate2>256</xcoordinate2><ycoordinate2>76</ycoordinate2><description></description><entry>4</entry><exit>2</exit></bus><bus><number>12</number><name></name><bits>1</bits><xcoordinate>242</xcoordinate><ycoordinate>91</ycoordinate><xcoordinate2>242</xcoordinate2><ycoordinate2>106</ycoordinate2><description></description><entry>2</entry><exit>0</exit></bus><bus><number>13</number><name></name><bits>1</bits><xcoordinate>242</xcoordinate><ycoordinate>106</ycoordinate><xcoordinate2>176</xcoordinate2><ycoordinate2>106</ycoordinate2><description></description><entry>12</entry><exit>0</exit></bus><bus><number>14</number><name></name><bits>1</bits><xcoordinate>194</xcoordinate><ycoordinate>106</ycoordinate><xcoordinate2>194</xcoordinate2><ycoordinate2>62</ycoordinate2><description></description><entry>13</entry><exit>0</exit></bus><bus><number>15</number><name></name><bits>1</bits><xcoordinate>194</xcoordinate><ycoordinate>62</ycoordinate><xcoordinate2>156</xcoordinate2><ycoordinate2>62</ycoordinate2><description></description><entry>14</entry><exit>0</exit></bus><bus><number>16</number><name></name><bits>1</bits><xcoordinate>168</xcoordinate><ycoordinate>62</ycoordinate><xcoordinate2>168</xcoordinate2><ycoordinate2>72</ycoordinate2><description></description><entry>15</entry><exit>1</exit></bus><bus><number>17</number><name>set</name><bits>1</bits><xcoordinate>140</xcoordinate><ycoordinate>44</ycoordinate><xcoordinate2>140</xcoordinate2><ycoordinate2>72</ycoordinate2><description></description><entry>3</entry><exit>1</exit></bus></processor>"
	};
	
	Logicmaker logicmaker;
	public LinearLayout helplayout;
	public ScrollView scroll;
	public HelpView(final Context context) {
		super(context);
		logicmaker=(Logicmaker)context;
		
		helplayout=new LinearLayout(context);
		helplayout.setOrientation(LinearLayout.VERTICAL);
		helplayout.addView(makeButton("What is LogicMaker",Color.argb(255, 200, 0, 0),new OnClickListener(){
			public void onClick(View v) {
				infoBox("LogicMaker",infoString);
			}}));
		helplayout.addView(makeButton("How do I...",Color.argb(255, 0, 200, 0),new OnClickListener(){
			public void onClick(View v) {
				LinearLayout howlayout=new LinearLayout(logicmaker);
				howlayout.setOrientation(LinearLayout.VERTICAL);
				for (int i=0; i<howDoIString.length; i+=2)
				{
					System.out.println(howDoIString[i]);
					final int j=i;
					howlayout.addView(makeButton(howDoIString[i],Color.argb(255, 200, 0, 0),new OnClickListener(){
						public void onClick(View v) {
							infoBox(howDoIString[j],howDoIString[j+1]);
						}}));					
				}
				scroll.removeView(helplayout);
				scroll.addView(howlayout);
			}}));
		helplayout.addView(makeButton("Types of components",Color.argb(255, 0, 0, 200),new OnClickListener(){
			public void onClick(View v) {
				LinearLayout howlayout=new LinearLayout(context);
				howlayout.setOrientation(LinearLayout.VERTICAL);
				for (int i=0; i<blockString.length; i+=2)
				{
					final int j=i;
					howlayout.addView(makeButton(blockString[i],Color.argb(255, 0, 0, 200),new OnClickListener(){
						public void onClick(View v) {
							infoBox(blockString[j],blockString[j+1]);
						}}));					
				}
				scroll.removeView(helplayout);
				scroll.addView(howlayout);
			}}));
		helplayout.addView(makeButton("Examples",Color.argb(255, 200, 200, 0),new OnClickListener(){
			public void onClick(View v) {
				LinearLayout howlayout=new LinearLayout(context);
				howlayout.setOrientation(LinearLayout.VERTICAL);
				for (int i=0; i<examples.length; i+=2)
				{
					final int j=i;
					howlayout.addView(makeButton(examples[j],Color.argb(255, 0, 0, 200),new OnClickListener(){
						public void onClick(View v) {
							logicmaker.datapath.clearAll();
							logicmaker.datapath.doloadxml(examples[j+1]);
							infoBox(""+examples[j]+" created","Press simulate to start");
						}}));					
				}
				scroll.removeView(helplayout);
				scroll.addView(howlayout);
			}}));
		helplayout.addView(makeButton("Credits",Color.argb(255, 200, 0, 200),new OnClickListener(){
			public void onClick(View v) {
				infoBox("About",aboutString);
			}}));
		scroll=new ScrollView(context);
		scroll.addView(helplayout);
	}
	private Button makeButton(String text, int color, OnClickListener listener)
	{
		Button b=new Button(logicmaker);
		b.setText(text);
		b.setOnClickListener(listener);
		b.setBackgroundColor(color);
		b.setTextColor(Color.WHITE);
		b.setTextSize(30);
		return b;
	}
	private void infoBox(String title, String message)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(logicmaker);
		builder.setTitle(title);
		builder.setCancelable(true);

		builder.setMessage(message);
		
		builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				}
			});
		AlertDialog box = builder.create();
		box.show();
	}
}
