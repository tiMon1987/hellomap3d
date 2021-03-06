package com.nutiteq.editable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.nutiteq.components.Components;
import com.nutiteq.components.Envelope;
import com.nutiteq.components.MutableEnvelope;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.Projection;
import com.nutiteq.vectorlayers.GeometryLayer;

/**
 * This is used for virtual editing circles - markers which are shown in corners of Lines and Polygons
 * 
 * @author mtehver
 *
 */
public class OverlayLayer extends GeometryLayer {
	List<Geometry> elements = new ArrayList<Geometry>();

	public OverlayLayer(Projection projection) {
		super(projection);
	}

	public List<Geometry> getAll() {
		modifyLock.lock();
		try {
			return new ArrayList<Geometry>(elements);
		}
		finally {
			modifyLock.unlock();
		}
	}

	public void setAll(List<? extends Geometry> elements) {
		for (Geometry element : this.elements) {
			if (!elements.contains(element)) {
				element.detachFromLayer();
			}
		}
		for (Geometry element : elements) {
			element.attachToLayer(this);
			element.setActiveStyle(getCurrentZoomLevel());
		}
		modifyLock.lock();
		try {
			this.elements = new ArrayList<Geometry>(elements);
			setVisibleElementsList(this.elements);
		}
		finally {
			modifyLock.unlock();
		}
	}

	@Override
	public void clear() {
		modifyLock.lock();
		try {
			this.elements.clear();
			setVisibleElementsList(this.elements);
		}
		finally {
			modifyLock.unlock();
		}

		for (Geometry element : this.elements) {
			element.detachFromLayer();
		}
	}

	@Override
	public void addAll(Collection<? extends Geometry> elements) {
		for (Geometry element : elements) {
			element.attachToLayer(this);
			element.setActiveStyle(getCurrentZoomLevel());
		}

		modifyLock.lock();
		try {
			this.elements.addAll(elements);
			setVisibleElementsList(this.elements);
		}
		finally {
			modifyLock.unlock();
		}
	}

	@Override
	public void removeAll(Collection<? extends Geometry> elements) {
		modifyLock.lock();
		try {
			this.elements.removeAll(elements);
			setVisibleElementsList(this.elements);
		}
		finally {
			modifyLock.unlock();
		}

		for (Geometry element : elements) {
			element.detachFromLayer();
		}
	}

	@Override
	public Envelope getDataExtent() {
		MutableEnvelope envelope = new MutableEnvelope(super.getDataExtent());
		modifyLock.lock();
		try {
			for (Geometry element : elements) {
				Envelope internalEnv = element.getInternalState().envelope;
				envelope.add(projection.fromInternal(internalEnv.minX, internalEnv.minY));
				envelope.add(projection.fromInternal(internalEnv.maxX, internalEnv.minY));
				envelope.add(projection.fromInternal(internalEnv.maxX, internalEnv.maxY));
				envelope.add(projection.fromInternal(internalEnv.minX, internalEnv.maxY));
			}
		}
		finally {
			modifyLock.unlock();
		}
		return new Envelope(envelope);
	}

	@Override
	public void elementUpdated(VectorElement element) {
		if (element instanceof Geometry) {
			element.calculateInternalState();
			Components components = getComponents();
			if (components != null) {
				components.mapRenderers.getMapRenderer().requestRenderView();
			}
		} else {
			super.elementUpdated(element);
		}
	}

	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		modifyLock.lock();
		try {
			setVisibleElementsList(elements);
		}
		finally {
			modifyLock.unlock();
		}
	}
}
