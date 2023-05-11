# hi_simulator
Hybrid Intelligence Simulator based on JaCaMo Multi-Agent System Framework

---

### IT artefact developed in context of the master thesis:

## MODELLING COGNITIVE TASKS FOR SIMULATING THE EFFECTS OF HYBRID INTELLIGENCE USING MULTI-AGENT SYSTEMS

### Ben Schlup (ben.schlup@schlup.com)

---
The paper describing this work will be made available after the university has accepted the thesis for the Master's degree. If you require any information with respect to this project prior to publication of the dissertation, please contact ben.schlup@schlup.com.  

The code was developed on JaCaMo v1.1 and tested successfully with JaCaMo v1.2. The folder structure fits into the Docker version of JaCaMo, which can be found here: https://github.com/jacamo-lang/docker. On Windows, using the JaCaMo docker image, a new project can be created in an empty base folder like this:
````
docker run -ti --rm -v "%cd%:/app" jomifred/jacamo:1.1 jacamo-new-project /app/hi_simulator
````
After that, the files from this repository [hi_simulator](https://github.com/benschlup/hi_simulator) must be merged into the default folder structure created by JaCaMo. 

**Note**:  
Apart from JaCaMo v1.1 or v1.2, snakeyaml is required. During initial development and test, snakeyaml v1.33 (https://mvnrepository.com/artifact/org.yaml/snakeyaml/1.33) was put into the /lib folder.
