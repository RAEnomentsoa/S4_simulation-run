technology: JAVA SWING askip


build a car simulation run with swing 

on the top there is a simulation of the car moving

car------------------------------------------------arivee
                     400m


car:
   config: 
        -capacity of scceleration /second
        -la vitesse maximum (maximum speed) 
      
    we will keep(stocker) the informstions in a .txt file ex car and config
    

- and add a bar km board kilometric in cars to mesure speed km/h in the middle

- on the left there will be a timer starting - 5 s with a start,stop and reset button


- on the right side there will be an accelerate button (witch accelarate the car if i maintain it) and a result how many second the car finiched the 400m if it finished it 


scenario:
    - the car accelerates when i hold the acceleration buton according to (capacity of scceleration /second),and when i release the acceleration the car goes at that constant speed until i accelerate again , the bord kilometric is constant,now if the maximum speed is reached the car doest accellerate anymore but stays at that constant max speed even if i accelerate

    rule: if capacity of scceleration /second is 15 km/s the accelerate 15 km /s 

    NB:note that  simulation of the car moving is moving according to scene and time , be realistic with the calculations

