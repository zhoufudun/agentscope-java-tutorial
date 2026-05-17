/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter10.model;

/**
 * Document processing status enumeration.
 *
 * <p>Represents the lifecycle states of a document in the RAG system:
 * <ul>
 *   <li>PENDING - Document uploaded, waiting to be processed</li>
 *   <li>PROCESSING - Document is being chunked and embedded</li>
 *   <li>COMPLETED - Document successfully processed and stored</li>
 *   <li>FAILED - Processing failed, check errorMessage for details</li>
 * </ul>
 */
public enum DocumentStatus {

    /**
     * Document uploaded, waiting to be processed.
     */
    PENDING,

    /**
     * Document is being chunked and embedded.
     */
    PROCESSING,

    /**
     * Document successfully processed and stored in vector database.
     */
    COMPLETED,

    /**
     * Processing failed, check errorMessage for details.
     */
    FAILED
}