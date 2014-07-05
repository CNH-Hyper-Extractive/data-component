data-component
===

A Distributed Data Component for the Open Modeling Interface

### overview

Data management is a fundamental part of modeling and simulation. Traditional approaches rely on input/output files and often require ad-hoc automation techniques to prepare and collect these files, in both desktop computing and grid computing in which models execute on clusters of connected computers. Through the adoption of standards, in both model component input-output interfaces and web service application programming interfaces, model input data can be retrieved from online data sources and output data delivered to online repositories via a general-purpose data component. Such a data component could be configured, and reconfigured, to serve as an intermediary between any model component and any web service adhering to the standards supported by the component. We have developed a distributed data component that conforms to the Open Modeling Interface (OpenMI) that both provides input data to model components retrieved from standards-based web services and delivers model output data to such services. The data component employs three techniques to achieve efficient operation: caching, prefetching, and buffering, which have been tailored to the unique design of the OpenMI.

### references

Bulatewicz, T., D. Andresen, S. Auvenshine, J. Peterson, D.R. Steward, A distributed data component for the Open Modeling Interface, Environmental Modelling & Software, Volume 57, July 2014, Pages 138-151, ISSN 1364-8152. http://www.sciencedirect.com/science/article/pii/S1364815214000772

Bulatewicz, T. and D. Andresen. 2012. Efficient data collection from Open Modeling Interface (OpenMI) components. In: Proceedings of the International Conference on Parallel and Distributed Processing Techniques and Applications (PDPTA) Volume 1, ed. H. R. Arabnia, CSREA Press, Las Vegas, Nevada, USA, July 16-19. 53–59. https://krex.k-state.edu/dspace/handle/2097/15197

Bulatewicz, T. and D. Andresen. 2011. Efficient data access for open modeling interface (OpenMI) components. In: Proceedings of the International Conference on Parallel and Distributed Processing Techniques and Applications (PDPTA) Volume 1, ed. H. R. Arabnia, CSREA Press, Las Vegas, Nevada, USA, July 18-21. 822–828. http://krex.k-state.edu/dspace/handle/2097/13097

### using the sample

The `sample` folder includes a simple composition (for OpenMI 1.4) consisting of a test component and the data component. The test component makes a series of requests to the data component which is configured to retrieve data from the National Weather Information Service (NWIS) WaterOneFlow web service.

To run the sample, first edit the `DataComponent.omi` file and set the `param_instance_address` to the IP address of your computer. Next, execute the DataStore.jar program (which requires that you have a Java Runtime 1.6 or higher installed) from the command prompt (so that you can monitor its output). Next, run the OpenMI 1.4 Configuration Editor application and open the Composition.opr file. Then run the composition and wait for it to complete. It should finish successfully and write a copy of the retieved data to log files in the `SampleComponent` folder.
