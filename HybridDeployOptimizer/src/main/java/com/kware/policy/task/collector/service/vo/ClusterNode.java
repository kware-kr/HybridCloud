package com.kware.policy.task.collector.service.vo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kware.policy.task.common.constant.StringConstant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClusterNode extends ClusterDefault {
	private String uid;
	private Integer clUid;
	private String nm;
	private String info;
	private String memo;
	private String hashVal;
	private String gpuinfo;

	private String gpuDriverString;
	private String cudaString;

	@JsonIgnore
	@Setter(AccessLevel.NONE)
	private GpuDriverVersion gpuDriverVersion = null;

	@JsonIgnore
	@Setter(AccessLevel.NONE)
	private CudaVersion cudaVersion = null;

	private Boolean status;
	private Map<String, String> labels = new HashMap<String, String>();

	public void setStatus(String _status) {
		if (StringConstant.STR_true.equals(_status)) {
			this.status = true;
		} else
			this.status = false;
	}

	public void setStatus(Boolean _status) {
		this.status = _status;
	}

	@Override
	public String getUniqueKey() {
		return clUid + "_" + nm;
	}

	public void setGpuDriverString(String gpuDriverString) {
		if (gpuDriverString == null)
			return;
		gpuDriverString = gpuDriverString.trim();

		this.gpuDriverString = gpuDriverString;
		gpuDriverVersion = new GpuDriverVersion(this.gpuDriverString);
	}

	public void setCudaString(String cudaString) {
		if (cudaString == null)
			return;
		cudaString = cudaString.trim();

		this.cudaString = cudaString;
		cudaVersion = new CudaVersion(this.cudaString);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(uid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClusterNode other = (ClusterNode) obj;
		return Objects.equals(uid, other.uid);
	}

	public class GpuDriverVersion implements Comparable<GpuDriverVersion> {
		private int major;
		private int minor;
		private int patch;

		public GpuDriverVersion(String version) {
			String[] parts = version.split("\\.");
			if (parts.length != 3) {
				throw new IllegalArgumentException("Invalid version format. Must be in 'major.minor.patch' format.");
			}
			try {
				this.major = Integer.parseInt(parts[0]);
				this.minor = Integer.parseInt(parts[1]);
				this.patch = Integer.parseInt(parts[2]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid version format. Version parts must be integers.");
			}
		}

		@Override
		public int compareTo(GpuDriverVersion other) {
			if (this.major != other.major) {
				return this.major - other.major;
			}
			if (this.minor != other.minor) {
				return this.minor - other.minor;
			}
			return this.patch - other.patch;
		}

		@Override
		public String toString() {
			return major + "." + minor + "." + patch;
		}
	}

	public class CudaVersion implements Comparable<CudaVersion> {
		private int major;
		private int minor;

		public CudaVersion(String version) {
			String[] parts = version.split("\\.");
			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid version format. Must be in 'major.minor' format.");
			}
			try {
				this.major = Integer.parseInt(parts[0]);
				this.minor = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid version format. Version parts must be integers.");
			}
		}

		@Override
		public int compareTo(CudaVersion other) {
			if (this.major != other.major) {
				return this.major - other.major;
			}
			return this.minor - other.minor;
		}

		@Override
		public String toString() {
			return major + "." + minor;
		}
	}
}