/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.component.validator.rules;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jetel.component.validator.AbstractValidationRule;
import org.jetel.component.validator.GraphWrapper;
import org.jetel.component.validator.ReadynessErrorAcumulator;
import org.jetel.component.validator.ValidationErrorAccumulator;
import org.jetel.component.validator.params.BooleanValidationParamNode;
import org.jetel.component.validator.params.StringEnumValidationParamNode;
import org.jetel.component.validator.params.ValidationParamNode;
import org.jetel.component.validator.utils.CommonFormats;
import org.jetel.component.validator.utils.ValidatorUtils;
import org.jetel.data.DataField;
import org.jetel.data.DataRecord;
import org.jetel.data.Defaults;
import org.jetel.metadata.DataFieldFormatType;
import org.jetel.metadata.DataFieldType;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.util.string.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author drabekj (info@cloveretl.com) (c) Javlin, a.s. (www.cloveretl.com)
 * @created 10.3.2013
 */
@XmlRootElement(name="date")
@XmlType(propOrder={"trimInput", "format", "locale", "timezone"})
public class DateValidationRule extends AbstractValidationRule {
	
	public static final int ERROR_DOUBLE_CHECK = 301;
	public static final int ERROR_PARSING = 302;
	
	@XmlElement(name="trimInput",required=false)
	protected BooleanValidationParamNode trimInput = new BooleanValidationParamNode(false);
	
	@XmlElement(name="format",required=true)
	private StringEnumValidationParamNode format = new StringEnumValidationParamNode(CommonFormats.defaultDate);
	
	@XmlElement(name="locale",required=true)
	private StringEnumValidationParamNode locale = new StringEnumValidationParamNode(Defaults.DEFAULT_LOCALE);
	
	@XmlElement(name="timezone",required=true)
	private StringEnumValidationParamNode timezone = new StringEnumValidationParamNode(Calendar.getInstance().getTimeZone().getID());
	
	public List<ValidationParamNode> initialize(DataRecordMetadata inMetadata, GraphWrapper graphWrapper) {
		ArrayList<ValidationParamNode> params = new ArrayList<ValidationParamNode>();
		trimInput.setName("Trim input");
		trimInput.setTooltip("Trim input before validation.");
		params.add(trimInput);
		format.setName("Format mask");
		format.setPlaceholder("Date format, for syntax see documentation");
		format.setOptions(CommonFormats.dates);
		params.add(format);
		locale.setName("Locale");
		locale.setOptions(CommonFormats.locales);
		locale.setTooltip("Locale code of record field");
		params.add(locale);
		timezone.setName("Timezone");
		timezone.setOptions(CommonFormats.timezones);
		timezone.setTooltip("Timezone code of record field");
		params.add(timezone);
		return params;
	}

	@Override
	public TARGET_TYPE getTargetType() {
		return TARGET_TYPE.ONE_FIELD;
	}

	@Override
	public State isValid(DataRecord record, ValidationErrorAccumulator ea, GraphWrapper graphWrapper) {
		if(!isEnabled()) {
			logNotValidated("Rule not enabled.");
			return State.NOT_VALIDATED;
		}
		logParams(StringUtils.mapToString(getProcessedParams(record.getMetadata(), graphWrapper), "=", "\n"));
		
		DataField field = record.getField(target.getValue());
		if(field.isNull()) {
			logSuccess("Field '" + target.getValue() + "' is null.");
			return State.VALID;
		}
		
		if(field.getMetadata().getDataType() != DataFieldType.STRING) {
			logError("Field '" + target.getValue() + "' is not a string.");
			return State.INVALID;
		}
		
		String tempString = field.toString();
		if(trimInput.getValue()) {
			tempString = tempString.trim();
		}
		DataFieldFormatType formatType = DataFieldFormatType.getFormatType(format.getValue());
	
		Locale realLocale = ValidatorUtils.localeFromString(locale.getValue());
		if(formatType == DataFieldFormatType.JAVA || formatType == null) {
			try {
				SimpleDateFormat dateFormat;
				if (formatType == null) {
					dateFormat = new SimpleDateFormat(Defaults.DEFAULT_DATETIME_FORMAT, realLocale);
				} else {
					dateFormat = new SimpleDateFormat(formatType.getFormat(format.getValue()), realLocale);
				}
				dateFormat.setTimeZone(TimeZone.getTimeZone(timezone.getValue()));
				
				Date parsedDate = dateFormat.parse(tempString);
				if(!dateFormat.format(parsedDate).equals(tempString.trim())) {
					logError("Field '" + target.getValue() + "' parsed as '" + parsedDate.toString() + "' is not a date with given settings (double check failed).");
					raiseError(ea, ERROR_DOUBLE_CHECK, "The target field is not correct date, double check failed.", target.getValue(), tempString);
					return State.INVALID;
				}
				logSuccess("Field '" + target.getValue() + "' parsed as '" + parsedDate.toString() + "' is date with given settings.");
				return State.VALID;
			} catch (Exception ex) {
				logError("Field '" + target.getValue() + "' with value '" + tempString + "' is not a date with given settings.");
				raiseError(ea, ERROR_PARSING, "The target field could not be parsed.", target.getValue(), tempString);
				return State.INVALID;	
			}
		} else {
			try {
				DateTimeFormatter formatter = DateTimeFormat.forPattern(formatType.getFormat(format.getValue()));
				formatter = formatter.withLocale(realLocale);
				formatter = formatter.withZone(DateTimeZone.forID(timezone.getValue()));
				DateTime parsedDate = formatter.parseDateTime(tempString);
				if(!parsedDate.toString(formatter).equals(tempString.trim())) {
					logError("Field '" + target.getValue() + "' parsed as '" + parsedDate.toString() + "' is not a date with given settings (double check failed).");
					raiseError(ea, ERROR_DOUBLE_CHECK, "The target field is not correct date, double check failed.", target.getValue(), tempString);
					return State.INVALID;
				}
				logSuccess("Field '" + target.getValue() + "' parsed as '" + parsedDate.toString() + "' is date with given settings.");
				return State.VALID;
			} catch (Exception ex) {
				logError("Field '" + target.getValue() + "' with value '" + tempString + "' is not a date with given settings.");
				raiseError(ea, ERROR_PARSING, "The target field could not be parsed.", target.getValue(), tempString);
				return State.INVALID;
			}
		}
	}

	@Override
	public boolean isReady(DataRecordMetadata inputMetadata, ReadynessErrorAcumulator accumulator) {
		if(!isEnabled()) {
			return true;
		}
		boolean state = true;
		if(target.getValue().isEmpty()) {
			accumulator.addError(target, this, "Target is empty.");
			state = false;
		} else {
			if(inputMetadata.getField(target.getValue()) != null && inputMetadata.getField(target.getValue()).getDataType() != DataFieldType.STRING) {
				accumulator.addError(target, this, "Target field is not string.");
				state = false;	
			}
		}
		if(!ValidatorUtils.isValidField(target.getValue(), inputMetadata)) { 
			accumulator.addError(target, this, "Target field is not present in input metadata.");
			state = false;
		}
		if(locale.getValue().isEmpty()) {
			accumulator.addError(locale, this, "Locale is empty.");
			state = false;
		}
		if(timezone.getValue().isEmpty()) {
			accumulator.addError(timezone, this, "Timezone is empty.");
			state = false;
		}
		DataFieldFormatType formatType = DataFieldFormatType.getFormatType(format.getValue());
		if(formatType == DataFieldFormatType.JAVA || formatType == null) {
			try {
				new SimpleDateFormat(formatType.getFormat(format.getValue()));
			} catch (Exception ex) {
				accumulator.addError(format, this, "Format mask is not in java format syntax.");
				state = false;	
			}
		} else if(formatType == DataFieldFormatType.JODA) {
			try {
				DateTimeFormat.forPattern(formatType.getFormat(format.getValue()));
			} catch (Exception ex) {
				accumulator.addError(format, this, "Format mask is not in joda format syntax.");
				state = false;
			}
		} else {
			accumulator.addError(format, this, "Unknown format mask prefix.");
			state = false;
		}
		return state;
	}

	@Override
	public String getCommonName() {
		return "Date";
	}

	@Override
	public String getCommonDescription() {
		return "Checks whether chosen field is a date in provided format.";
	}
	
	public StringEnumValidationParamNode getFormat() {
		return format;
	}

	public StringEnumValidationParamNode getLocale() {
		return locale;
	}

	public StringEnumValidationParamNode getTimezone() {
		return timezone;
	}

}
