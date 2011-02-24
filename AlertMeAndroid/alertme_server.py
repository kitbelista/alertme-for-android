from SimpleXMLRPCServer import SimpleXMLRPCServer
import datetime, socket, xmlrpclib, random, array

# Create dummy AlertMe server
server = SimpleXMLRPCServer(("localhost", 8888))

# This is a basic interface to the AlertMe API for development purposes
# As such, this does not implement all the functions but poses as a
# means of testing before actual hits to the API (where you may be
# blocked accidently during testing)
#
# This provides access to 3 dummy accounts with usernames test1, test2
# and test3 (no password has been checked). Sessions are not timed
# (future upgrade to this?) but each account has their own hub(s) to
# control and as long as this server it running they keep state.
# All hubs start with state 'Home'
#
class MyFuncs:
	acc1 = "test1"
	pass1 = "pass1"
	mode1 = 0
	acc1hubs = "Firsthouse|test1hub"

	acc2 = "test2"
	pass2 = "pass2"
	mode2 = 0
	mode2_h1 = 0
	mode2_h2 = 0
	acc2hub = 1 
	acc2hubs = "Foo House|test2hub1,Cottage|test2hub2"

	acc3 = "test3"
	pass3 = "pass3"
	mode3 = 0
	acc3hubs = "My House|test3hub"
	def login(self, user, passwd, api):
		res = ""
		if (user==self.acc1):
			res = self.pass1
		elif (user==self.acc2):
			res = self.pass2
		elif (user==self.acc3):
			res = self.pass3
			
		return res
	def getUserInfo(self, sessionKey):
		res = ""
		if (sessionKey==self.pass1):
			res = "firstname|Agent,lastname|Smith,username|alertme.com@matrix.com"
		elif (sessionKey==self.pass2):
			res = "firstname|Barry,lastname|Foo,username|alertme.com@foo.com"
		elif (sessionKey==self.pass3):
			res = "firstname|Lol,lastname|Katz,username|alertme.com@lol.com"
		return res
	def getAllDevices(self, sessionKey):
		res = ""
		if (sessionKey==self.pass1):
			res = ""
			res += "Button|Z1-G8-33-1D-Button|Button,"
			res += "Front Door|Z1-G8-33-1D-Contact1|Contact Sensor,"
			res += "Back Door|Z1-G8-33-1D-Contact2|Contact Sensor,"
			res += "Mid Door|Z1-G8-33-1D-Contact3|Contact Sensor,"
			res += "Neo|Z1-G8-33-1D-Keyfob1|Keyfob,"
			res += "Agent Smith|Z1-G8-33-1D-Keyfob2|Keyfob,"
			res += "Spare|Z1-G8-33-1D-Keyfob3|Keyfob,"
			res += "Backrooms|Z1-G8-33-1D-Motion1|Motion Sensor,"
			res += "Upstairs|Z1-G8-33-1D-Motion2|Motion Sensor,"
			res += "Study|Z1-G8-33-1D-Motion3|Motion Sensor,"
			res += "Front Hall|Z1-G8-33-1D-Motion4|Motion Sensor,"
			res += "Bacon Room|Z1-G8-33-1D-Alarm1|Alarm Detector,"
			res += "Bed Room|Z1-G8-33-1D-Alarm2|Alarm Detector,"
			res += "Lampy Lamp|Z1-G8-33-1D-L4mp|Lamp,"
			res += "Smarty Plug|Z1-G8-33-1D-P0werPlug|Power Controller,"
			res += "Meter Reader|Z1-G8-33-1D-Meter|Powerclamp"
		elif (sessionKey==self.pass2):
			if (self.acc2hub == 1):
				res = ""
				res += "Button|Z1-G8-33-1D-Button|Button,"
				res += "Front Door|Z1-G8-33-1D-Contact1|Contact Sensor,"
				res += "Back Door|Z1-G8-33-1D-Contact2|Contact Sensor,"
				res += "Spare|Z1-G8-33-1D-Keyfob3|Keyfob,"
				res += "Front Hall|Z1-G8-33-1D-Motion4|Motion Sensor,"
				res += "Bed Room|Z1-G8-33-1D-Alarm2|Alarm Detector,"
				res += "My Lamp|Z1-G8-33-1D-L4mp|Lamp"
			elif (self.acc2hub == 2):
				res = ""
				res += "Bell|Z1-G8-33-1D-Button|Button,"
				res += "Door|Z1-G8-33-1D-Contact1|Contact Sensor,"
				res += "Cottager|Z1-G8-33-1D-Keyfob3|Keyfob,"
				res += "Study|Z1-G8-33-1D-Motion4|Motion Sensor,"
				res += "Cellar|Z1-G8-33-1D-Alarm2|Alarm Detector,"
				res += "Security Lamp|Z1-G8-33-1D-L4mp|Lamp"
		elif (sessionKey==self.pass3):
			res = ""
			res += "Main Door|Z1-G8-33-1D-Contact1|Contact Sensor,"
			res += "Side Door|Z1-G8-33-1D-Contact2|Contact Sensor,"
			res += "Bob|Z1-G8-33-1D-Keyfob3|Keyfob,"
			res += "Front Hall|Z1-G8-33-1D-Motion4|Motion Sensor,"
			res += "Room|Z1-G8-33-1D-Alarm2|Alarm Detector,"
			res += "Lamp|Z1-G8-33-1D-L4mp|Lamp,"
			res += "Powerplug|Z1-G8-33-1D-P0werPlug|Power Controller,"
			res += "Meter|Z1-G8-33-1D-Meter|Powerclamp"
		return res
	def setHub(self, sessionKey, hubID):
		res = ""
		if (sessionKey==self.pass1):
			if (hubID=="test1hub"):
				res = "ok"
		elif (sessionKey==self.pass2):
			if (hubID=="test2hub1"):
				if (self.acc2hub == 2):
					self.mode2_h2 = self.mode2
					self.mode2 = self.mode2_h1
				self.acc2hub = 1
				res = "ok"
			elif (hubID=="test2hub2"):
				if (self.acc2hub == 1):
					self.mode2_h1 = self.mode2
					self.mode2 = self.mode2_h2
				self.acc2hub = 2
				res = "ok"
		elif (sessionKey==self.pass3):
			if (hubID=="test3hub"):
				res = "ok"

		return res
	def getAllHubs(self, sessionKey):
		res = ""
		if (sessionKey==self.pass1):
			res = self.acc1hubs
		elif (sessionKey==self.pass2):
			res = self.acc2hubs
		elif (sessionKey==self.pass3):
			res = self.acc3hubs
		return res
	def getBehaviour(self, sessionKey):
		themode = 0
		res = ""
		if (sessionKey==self.pass1):
			themode = self.mode1
		elif (sessionKey==self.pass2):
			themode = self.mode2
		elif (sessionKey==self.pass3):
			themode = self.mode3
		if (themode==0):
			res = "Home"
		elif (themode==1):
			res = "Away"
		elif (themode==2):
			res = "Night"
		return res
	def getAllServiceStates(self, sessionKey, service):
		res = ""
		if (service=="IntruderAlarm"):
			res = "disarmed,queryError,error,armGrace,armed,alarmGrace,alarmed,serverAlarmCleared,alarmWarning"
		if (service=="Doorbell"):
			res = "idle,ringing"
		if (service=="EmergencyAlarm"):
			res = "passive,alarmConfirm,alarmGrace,alarmed,serverAlarmCleared,alarmWarning"
		return res
	def getAllServices(self):
		res = ""
		if (sessionKey==self.pass1):
			res = "IntruderAlarm,EmergencyAlarm,Doorbell,Presence,Temperature,History,Sensors,PanicButton,EnergyMonitor,SecurityLight,TrackingService"
		elif (sessionKey==self.pass2):
			res = "IntruderAlarm,EmergencyAlarm,Presence,Temperature,History,Sensors,TrackingService"
		elif (sessionKey==self.pass3):
			res = "IntruderAlarm,EmergencyAlarm,Presence,Temperature,History,Sensors,TrackingService,EnergyMonitor,SecurityLight"
		return res
	def getDeviceChannelValue(self, sessionKey, deviceId, attribute=None):

		if (attribute==None):
			bat1 = random.random()
			if (bat1<0.60):
				bat1 = bat1 + 0.2
			battery = "{0}".format(2+bat1)
			temp = random.random()
			if (temp<0.30):
				temp = temp*100
			else:
				temp = temp*10
				temp = temp + 10
			temp = "{0}".format(temp)
			lqm = random.random()
			if (lqm<0.50):
				lqm = lqm*100
			else:
				lqm = lqm*10
			lqi = "{0}".format(100-lqm)
			return "upgrade|Ready,lqi|"+lqi+",tamper|False,Presence|True,temperature|"+temp+",closed|True,remotemode|True,relaystate|True,mainsstate|True,batteryLevel|"+battery
		print
		print "getDeviceChannelValue(self, sessionKey, deviceId, attribute) CALLED ("+sessionKey+","+attribute+")"
		print
		if (attribute=="Temperature"):
			temp = random.random()
			temp = temp*10
			temp = temp + 10
			temp = "{0}".format(temp)
			return temp
		if (attribute=="BatteryLevel"):
			bat1 = random.random()
			if (bat1<0.60):
				bat1 = bat1 + 0.2
			battery = "{0}".format(2+bat1)
			return battery
		if (attribute=="LQI"):
			lqm = random.random()
			if (lqm<0.50):
				lqm = lqm*100
			else:
				lqm = lqm*10
			lqi = "{0}".format(100-lqm)
			return lqi
		if (attribute=="Presence"):
			return "True"
		if (attribute=="RemoteMode"):
			return "False"
		if (attribute=="RelayState"):
			return "True"
		if (attribute=="PowerLevel"):
			return "257"
		return "error_nothing"
	#def getDeviceChannelValue(self, sessionKey, deviceId):
	#	print
#		print "getDeviceChannelValue(self, sessionKey, deviceId) CALLED ("+sessionKey+")"
	#	print
	#	return self.getDeviceChannelValue(sessionKey, deviceId, "")
	def getAllBehaviours(self, sessionKey):
		return "Home,Away,Night"
	def getHubStatus(self, sessionKey):
		res = "IsAvailable|Yes,IsUpgrading|No"
		return res
	def getEventLog(self, sessionKey, nullStr1, limit, nullStr2, nullStr3, nullStr4="false"):
		res = ""
		if (sessionKey==self.pass1):
			res = ""
			res += "1297278581|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|Agent Smith's Keyfob has arrived home,"
			res += "1297257679|zigbeeId|Z1-G8-33-1D-Keyfob3|devType|AMKeyFob|Spare's Keyfob has left home,"
			res += "1297257671|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|Agent Smith's Keyfob has left home,"
			res += "1297147836|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|Agent Smith's Keyfob has left home,"
			res += "1297146941|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|The Intruder Alarm was disarmed by Agent Smith's Keyfob,"
			res += "1297146940||Behaviour changed to At home,"
			res += "1297122356|zigbeeId|Z1-G8-33-1D-Keyfob1|devType|AMKeyFob|The Intruder Alarm was armed by Neo's Keyfob,"
			res += "1297122354||Behaviour changed to Night,"
			res += "1297105808|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|Agent Smith's Keyfob has arrived home,"
			res += "1297097585|zigbeeId|Z1-G8-33-1D-Keyfob1|devType|AMKeyFob|The Intruder Alarm was disarmed by Neo's Keyfob,"
			res += "1297097584||Behaviour changed to At home,"
			res += "1297097553|zigbeeId|Z1-G8-33-1D-Keyfob1|devType|AMKeyFob|The Intruder Alarm was armed by Neo's Keyfob,"
			res += "1297097551||Behaviour changed to Night,"
			res += "1297063050|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|Agent Smith's Keyfob has left home,"
			res += "1297059162|zigbeeId|Z1-G8-33-1D-Keyfob1|devType|AMKeyFob|The Intruder Alarm was disarmed by Neo's Keyfob,"
			res += "1297059161||Behaviour changed to At home,"
			res += "1297041038|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|The Intruder Alarm was armed by Agent Smith's Keyfob,"
			res += "1297041036||Behaviour changed to Night,"
			res += "1297030069|zigbeeId|Z1-G8-33-1D-Keyfob1|devType|AMKeyFob|Neo's Keyfob has arrived home,"
			res += "1297029997|zigbeeId|Z1-G8-33-1D-Keyfob2|devType|AMKeyFob|The Intruder Alarm was disarmed by Agent Smith's Keyfob"
		elif (sessionKey==self.pass2):
			res = ""
			res += "1297146940||Behaviour changed to At home,"
			res += "1297122354||Behaviour changed to Night,"
			res += "1297097584||Behaviour changed to At home,"
			res += "1297097551||Behaviour changed to Night,"
			res += "1297059161||Behaviour changed to At home,"
			res += "1297041036||Behaviour changed to Night,"
			res += "1297029997|zigbeeId|Z1-G8-33-1D-Keyfob3|devType|AMKeyFob|The Intruder Alarm was disarmed by Spare's Keyfob"
		elif (sessionKey==self.pass3):
			res = ""
			res += "1297146940||Behaviour changed to At home,"
			res += "1297122354||Behaviour changed to Night,"
			res += "1297097584||Behaviour changed to At home,"
			res += "1297041036|zigbeeId|Z1-G8-33-1D-Keyfob3|devType|AMKeyFob|The Intruder Alarm was disarmed by Bob's Keyfob,"
			res += "1297097551||Behaviour changed to Night,"
			res += "1297059161||Behaviour changed to At home,"
			res += "1297029997||Behaviour changed to Night"
		return res
	def sendCommand(self, sessionKey, command, cmode):
		res = ""
		if (sessionKey==self.pass1):
			res = "failed"
			ok = "ok"
			if (command=="IntruderAlarm"):
				if (self.mode1==0):
					if (cmode=="arm"):
						self.mode1=1
						res = ok
					if (cmode=="nightArm"):
						self.mode1=2
						res = ok
				else:
					if (self.mode1==1):
						if (cmode=="disarm"):
							self.mode1=0
							res = ok
					else:
						if (self.mode1==2):
							if (cmode=="disarm"):
								self.mode1=0
								res = ok
		elif (sessionKey==self.pass2):
			res = "failed"
			ok = "ok"
			if (command=="IntruderAlarm"):
				if (self.mode2==0):
					if (cmode=="arm"):
						self.mode2=1
						res = ok
					if (cmode=="nightArm"):
						self.mode2=2
						res = ok
				else:
					if (self.mode2==1):
						if (cmode=="disarm"):
							self.mode2=0
							res = ok
					else:
						if (self.mode2==2):
							if (cmode=="disarm"):
								self.mode2=0
								res = ok
		elif (sessionKey==self.pass3):
			res = "failed"
			ok = "ok"
			if (command=="IntruderAlarm"):
				if (self.mode3==0):
					if (cmode=="arm"):
						self.mode3=1
						res = ok
					if (cmode=="nightArm"):
						self.mode3=2
						res = ok
				else:
					if (self.mode3==1):
						if (cmode=="disarm"):
							self.mode3=0
							res = ok
					else:
						if (self.mode3==2):
							if (cmode=="disarm"):
								self.mode3=0
								res = ok
		return res
			

    
server.register_instance(MyFuncs())

# Run the server's main loop
server.serve_forever()
