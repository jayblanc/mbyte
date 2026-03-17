## MByte Extension's Projects

### Store creation task manager

#### Description

This project is responsible for managing the asynchronous tasks related to store creation. When a user requests the creation of a new store, this component handles the task in the background, ensuring that the store is set up correctly without blocking the user's main workflow.
A Task Manager is implemented to queue and process store creation requests efficiently. This allows for better resource management and improves the overall user experience by offloading time-consuming operations to a dedicated service.
In the GUI, users can initiate store creation requests, which are then processed by this task manager. Users can also monitor the status of their store creation tasks, receiving notifications upon completion or if any issues arise during the process.

--- 

### Multi-Store Support

#### Description

This project extends the existing MByte application to support multiple stores per user. Each user can now create and manage several stores, allowing for better organization and separation of data.
The multi-store functionality is integrated into the user interface, providing users with an intuitive way to switch between their different stores. Users can create new stores, rename them, and delete them as needed.

### Store configuration management

#### Description

This project focuses on the management of store configurations within the MByte application. It provides users with the ability to customize various settings related to their stores, such as storage limits or maximum file numbers.
The configuration management interface allows users to easily modify their store settings through a user-friendly GUI. 

### Failover and High Availability

#### Description

This project aims to enhance the reliability and availability of the MByte application by implementing failover mechanisms and high availability configurations.
By deploying redundant instances of critical components, the system can continue to operate seamlessly in the event of hardware failures or other disruptions.
Load balancing techniques are employed to distribute traffic evenly across multiple instances, ensuring optimal performance and responsiveness.

### Vault integration for secret management

#### Description

This project integrates HashiCorp Vault into the MByte application for secure secret management. Vault provides a centralized solution for storing and managing sensitive information such as API keys, database credentials, and encryption keys.
By leveraging Vault's robust security features, the MByte application can enhance its overall security posture, ensuring that secrets are protected and access is tightly controlled.
The integration includes mechanisms for retrieving secrets from Vault as needed, allowing the application to securely access sensitive information without hardcoding it into the codebase. 

### Multiple Server Deployment for stores

#### Description

This project enables the deployment of MByte stores across multiple servers, allowing for improved scalability and performance.
By distributing stores across different servers, the system can handle increased workloads and provide better response times for users.
The multi-server deployment is managed through a centralized configuration system, ensuring that stores are properly allocated and balanced across available resources. 
-6666
### Full Text Search Integration

#### Description

This project integrates full-text search capabilities into the MByte application, allowing users to perform advanced searches across their stored files and documents.
By leveraging search engines like Apache Lucene or Solr, users can quickly locate files based on keywords, phrases, or other criteria.
The search functionality is seamlessly integrated into the user interface, providing an intuitive way for users to access and utilize the search features.

### File Sharing

#### Description

This project introduces file sharing features to the MByte application, enabling users to share files and folders with others securely.
Users can generate shareable links and send them to collaborators, allowing for easy access to specific files or directories.
Access controls and permissions can be configured to ensure that shared content is only accessible to authorized users.

### Activity Logging and Auditing

#### Description

This project implements activity logging and auditing features within the MByte application. It tracks user actions and system events, providing a comprehensive audit trail for security and compliance purposes.
Administrators can review logs to monitor user activity, identify potential security issues, and ensure adherence to organizational policies.
The logging system is designed to be efficient and scalable, capable of handling large volumes of data without impacting system performance.

### File history (versioning)

#### Description

This project adds file versioning capabilities to the MByte application, allowing users to maintain a history of changes made to their files.
Each time a file is modified, a new version is created and stored, enabling users to revert to previous versions if needed.
The versioning system is integrated into the user interface, providing users with an easy way to view and manage file versions.

### Thumbnail generation and preview

#### Description

This project implements thumbnail generation and preview features for files stored in the MByte application. Users can view small previews of images and documents directly within the interface, enhancing the user experience.
Thumbnails are generated automatically when files are uploaded, allowing for quick visual identification of content without the need to open each file individually.
The generation is done asynchronously to ensure that it does not impact the performance of the main application.
A dedicated API endpoint is provided to retrieve thumbnails for display in the GUI.
If a thumbnail is not available, a default placeholder image is returned.

### FTP Protocol integration

#### Description

This project integrates FTP protocol support into the MByte application, allowing users to access and manage their files using standard FTP clients.
Users can connect to their stores via FTP, enabling them to upload, download, and organize files using familiar tools.
The FTP integration is designed to be secure and efficient, ensuring that user data is protected during transfers. 
The integration of a third-party library, such as Apache FtpServer, is utilized to handle FTP protocol operations.

### WebHook management

#### Description

This project introduces WebHook management capabilities to the MByte application, allowing users to set up and manage WebHooks for various events within their stores.
Users can create WebHooks that trigger HTTP callbacks to specified URLs when certain actions occur, such as file uploads or deletions.
The WebHook management interface provides users with an easy way to configure and monitor their WebHooks, ensuring that they can integrate MByte with other applications and services seamlessly.

### Integrity check

#### Description

This project implements integrity check features within the MByte application to ensure the consistency and reliability of stored files.
By calculating and storing checksums for files upon upload, the system can periodically verify that files have not been altered or corrupted.
The integrity check process runs in the background, scanning files and comparing their current checksums with the stored values.
If discrepancies are detected, the system can alert users or administrators, allowing them to take appropriate action to resolve potential issues.

### Quotas management

#### Description

This project focuses on implementing quotas management within the MByte application, allowing administrators to set and enforce storage limits for users and stores.
Users can view their current storage usage and remaining quota through the user interface, helping them manage their data effectively.
Administrators can configure quotas based on various criteria, such as user roles or store types, ensuring that resources are allocated fairly and efficiently. 
If quotas are exceeded, users receive notifications and may be restricted from uploading additional files until they free up space.

### Trash integration

#### Description

This project introduces a trash or recycle bin feature to the MByte application, allowing users to recover deleted files and folders.
When a user deletes an item, it is moved to the trash instead of being permanently removed, providing a safety net against accidental deletions.
Users can access the trash through the user interface, where they can view, restore, or permanently delete items.
The trash system is designed to manage storage efficiently, with options for automatic purging of old items based on configurable retention policies (30j).   

### Upload progress and resume download (HTTP Range Request)

#### Description

This project enhances the MByte application by implementing upload progress tracking and download resumption capabilities using HTTP Range Requests.
Users can monitor the progress of their file uploads in real-time, providing feedback on the status of their transfers.
For downloads, the application supports HTTP Range Requests, allowing users to resume interrupted downloads without starting from scratch.
This feature improves the overall user experience, especially for large files or in environments with unstable network connections. 

### Tagging / Automated tagging

#### Description

This project introduces tagging and automated tagging features to the MByte application, allowing users to organize and categorize their files more effectively.
Users can manually assign tags to files and folders, making it easier to search and filter content based on specific criteria.
Additionally, automated tagging capabilities analyze file metadata and content to suggest or apply relevant tags automatically.
The tagging system is integrated into the user interface and in the API, providing users with intuitive tools to manage their tags and enhance their file organization.

### External storage integration (S3, WebDAV, etc.) with ciphering

#### Description

This project introduces external storage integration capabilities to the MByte application, allowing users to connect and utilize various external storage services such as Amazon S3 and WebDAV.
Users can configure their stores to use these external storage backends, providing flexibility and scalability for their data storage needs.
To ensure data security, the integration includes ciphering mechanisms that encrypt files before they are stored externally.
The external storage integration is seamlessly integrated into the MByte application, allowing users to manage their files across different storage solutions while maintaining data confidentiality.

In an extended version of this project, goal is to be able to store data on multiple external storage backends with redundancy and load balancing. This involves distributing files across different storage services 
to enhance data availability and reliability. The system will intelligently manage file placement and retrieval, ensuring optimal performance and fault tolerance. Also, adding free small storage services like 
Google Drive, Dropbox, OneDrive as external storage options with multiple instances could provide a more cost-effective solution with unlimited storage capacity...

### Discord Bot integration for chat backup (text and files) and notifications

#### Description

This project integrates Discord Bot functionality into the MByte application, enabling users to back up chat messages (text and files) from Discord channels directly into their MByte stores.
The bot can be configured to monitor specific channels and automatically save messages and attachments to the user's designated store.
Additionally, the integration includes notification features that alert users of important events within their MByte stores via Discord messages.
This integration enhances the MByte application by providing seamless connectivity with Discord, allowing users to manage and back up their chat data efficiently.
