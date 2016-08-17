/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.niord.core.message;

import org.niord.core.model.BaseEntity;
import org.niord.core.model.IndexedEntity;
import org.niord.model.message.DateIntervalVo;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Represents a single date interval for a message
 */
@Entity
@SuppressWarnings("unused")
public class DateInterval extends BaseEntity<Integer> implements IndexedEntity, Comparable<DateInterval> {

    @NotNull
    @ManyToOne
    Message message;

    int indexNo;

    /**
     * The all-day flag can be used to signal to the UI that the date interval must be treated as an al-day
     * interval. So, the editor will force the time part of fromDate to "00:00:00" and the time part
     * of toDate to "23:59:59" in the current time zone.<br>
     * Similarly, when presenting the message details, only print out the date part and not the time part.<br>
     */
    Boolean allDay;

    @Temporal(TemporalType.TIMESTAMP)
    Date fromDate;

    @Temporal(TemporalType.TIMESTAMP)
    Date toDate;

    /** Constructor */
    public DateInterval() {
    }


    /** Constructor */
    public DateInterval(DateIntervalVo dateInterval) {
        this.allDay = dateInterval.getAllDay();
        this.fromDate = dateInterval.getFromDate();
        this.toDate = dateInterval.getToDate();
    }


    /** Updates this date interval from another **/
    public void updateDateInterval(DateInterval dateInterval) {
        this.indexNo = dateInterval.getIndexNo();
        this.allDay = dateInterval.getAllDay();
        this.fromDate = dateInterval.getFromDate();
        this.toDate = dateInterval.getToDate();
    }


    /** Converts this entity to a value object */
    public DateIntervalVo toVo() {
        DateIntervalVo dateInterval = new DateIntervalVo();
        dateInterval.setAllDay(allDay);
        dateInterval.setFromDate(fromDate);
        dateInterval.setToDate(toDate);
        return dateInterval;
    }


    /** Check that the date interval is valid */
    @PrePersist
    @PreUpdate
    public void checkDateInterval() {
        // To-date must not be before from-date
        if (fromDate != null && toDate != null && toDate.before(fromDate)) {
            toDate = fromDate;
        }
    }


    /** {@inheritDoc} **/
    @Override
    @SuppressWarnings("all")
    public int compareTo(DateInterval di) {
        if (fromDate == null && di.fromDate == null) {
            return 0;
        } else if (fromDate == null) {
            return -1;  // NB: null from-date before any other dates
        } else if (di.fromDate == null) {
            return 1;
        } else {
            int result = fromDate.compareTo(di.fromDate);
            if (result == 0) {
                if (toDate == null && di.toDate == null) {
                    return 0;
                } else if (toDate == null) {
                    return 1; // NB: null to-date after all other dates
                } else if (di.toDate == null) {
                    return -1;
                }
                result = toDate.compareTo(di.toDate);
            }
            return result;
        }
    }


    /*************************/
    /** Getters and Setters **/
    /*************************/

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public int getIndexNo() {
        return indexNo;
    }

    @Override
    public void setIndexNo(int indexNo) {
        this.indexNo = indexNo;
    }

    public Boolean getAllDay() {
        return allDay;
    }

    public void setAllDay(Boolean allDay) {
        this.allDay = allDay;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }
}
