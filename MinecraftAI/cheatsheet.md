#Minecraft Project Malmo Cheatsheet

##Commands

###Python
Python commands are issued using the `agent_host.sendCommand(String command)` format.

####Basic Movement
As a note, these commands continue as activated until stopped.
* `move [-1,1]`
  * `move 1` is full speed **forward**; `move -0.5` moves **backwards** at half speed.
* `strafe [-1,1]`
  * `strafe -1` moves **left** at full speed; `strafe 1` moves **right** at full speed.
* `pitch [-1,1]`
  * `pitch -1` starts tipping camera **upwards** at full speed, `pitch 0.1` starts looking **down** slowly.
* `turn [-1,1]`
  * `turn -1` starts turning full speed **left**.
* `jump 1/0`
  * `jump 1` **begins** jumping; `jump 0` ends it.
* `crouch 1/0`
* `attack 1/0`

####Items
* `use 1/0`
* `hotbar.n 1/0`
  * Presses the nth hotbar key or releases it
  * `hotbar.9 1` presses the 9th hotbar key; and `hotbar.9 0` releases it.
  * These commands are 1-indexed, not 0-indexed (like their corresponding inventory slots)

###XML
####Drawing
* `<Weather>type</Weather>` should be placed into thhe `ServerInitialCondition` section.
* `<DrawCuboid x1, y1, z1, x2, y2, z2, type/>`
* `<DrawLine x1, y1, z1, x2, y2, z2, type/>`
* `<DrawBlock x, y, z, type/>`
* `<DrawSphere x, y, z, radius, type/>`
* `<DrawItem x, y, z, type/>`
* This example code should be placed in the `ServerHandlers` block, right after the `WorldGenerator`:
  * ```
    <DrawingDecorator>
    <DrawSphere x="-27" y="70" z="0" radius="30" type="air"/>
    </DrawingDecorator>

####Inventory
* Adding to the Inventory section to the AgentStart node, after the Placement node. For example:
  * ```
    <Inventory>
      <InventoryItem slot="0" type="diamond_pickaxe"/>
    </Inventory>
  * There are 40 inventory slots in Minecraft, numbered 0-39:
    * 0-8 are the “hotbar” slots – they are displayed on the HUD and accessed with the hotbar
  keys, and can be selected by the agent using the “hotbar.x” command provided by the InventoryCommands.
    * Note: The slots are 0-indexed but the key commands are 1-indexed, so to select slot 8, the command sequence would be:
      * agent_host.sendCommand(“hotbar.9 1”) # press the key
      * agent_host.sendCommand(“hotbar.9 0”) # release the key
    * 9-35 are the three rows of items visible in the player’s inventory menu (press <E> within the
  game to view this)
    * 36-39 are reserved for the four armour slots (eg for diamond_helmet, etc)

For a list of the block and item types available, see [this file](Malmo-0.14.0-Mac-64bit/Schemas/Types.xsd).

###Some Useful Information
* `print my_mission.getAsXML(True)` will return the XML sent to Minecraft as the mission parameter
    * A sample of this XML is available in the `tutorial_2.py` python sample.
* Cardinal directions:
    * South is 0˚ yaw
    * West is 90˚ yaw
    * East is -90˚ yaw
    * North is 180˚ yaw